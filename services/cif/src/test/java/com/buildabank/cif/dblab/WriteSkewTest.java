// services/cif/src/test/java/com/buildabank/cif/dblab/WriteSkewTest.java
package com.buildabank.cif.dblab;

import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <strong>Write skew</strong> — the subtle anomaly that <em>survives</em> REPEATABLE READ and is the reason
 * SERIALIZABLE exists.
 *
 * <p>Scenario (a banking invariant): a customer has two linked accounts, A and B, with a shared-overdraft
 * rule — <em>their combined balance must never go below zero</em>. Both start at 100 (sum 200). Two
 * withdrawals of 150 run concurrently; each reads the sum (200), each decides "200 ≥ 150, fine", and each
 * debits a <em>different</em> account. Individually legal; together they leave the sum at −100.
 *
 * <p>Because each transaction writes a <em>different row</em>, there is no write-write conflict, so snapshot
 * isolation (Postgres REPEATABLE READ) lets both commit. Only SERIALIZABLE — via Postgres's
 * Serializable Snapshot Isolation (SSI), which tracks the read/write dependencies between the two — detects
 * the dangerous cycle and aborts one with SQLState {@code 40001}.
 *
 * <p>This is the <strong>§12.3 mutation point</strong> for the step: the only difference between the two
 * tests below is the isolation level. Weaken {@link #serializableRejectsTheWriteSkew()} from SERIALIZABLE to
 * REPEATABLE READ and its "conflict expected" assertion fails — proving the test really depends on the fix.
 */
class WriteSkewTest extends DbLab {

    @BeforeEach
    void reset() throws Exception {
        try (Connection c = openAuto()) {
            exec(c, "drop table if exists linked_account");
            exec(c, "create table linked_account (name text primary key, balance numeric not null)");
            exec(c, "insert into linked_account values ('A', 100), ('B', 100)");
        }
    }

    private long combinedBalance() throws Exception {
        try (Connection c = openAuto()) {
            return scalar(c, "select coalesce(sum(balance), 0) from linked_account");
        }
    }

    /** REPEATABLE READ does NOT stop write skew: both withdrawals commit and the invariant is violated. */
    @Test
    void repeatableReadAllowsWriteSkew_invariantViolated() throws Exception {
        try (Connection t1 = openTx(TRANSACTION_REPEATABLE_READ);
             Connection t2 = openTx(TRANSACTION_REPEATABLE_READ)) {

            // Both read the combined balance from their own snapshot: each sees 200, each passes the check.
            long sumSeenByT1 = scalar(t1, "select sum(balance) from linked_account");
            long sumSeenByT2 = scalar(t2, "select sum(balance) from linked_account");
            assertThat(sumSeenByT1).isEqualTo(200);
            assertThat(sumSeenByT2).isEqualTo(200);

            // Different rows → no write-write conflict → both commit.
            exec(t1, "update linked_account set balance = balance - 150 where name = 'A'");
            t1.commit();
            exec(t2, "update linked_account set balance = balance - 150 where name = 'B'");
            t2.commit();
        }

        long finalSum = combinedBalance();
        System.out.println("[write-skew @REPEATABLE READ] final combined balance = " + finalSum);
        assertThat(finalSum).isEqualTo(-100);   // the bug: invariant (>= 0) silently broken
    }

    /** SERIALIZABLE detects the read/write dependency cycle and aborts the second commit with 40001. */
    @Test
    void serializableRejectsTheWriteSkew() throws Exception {
        try (Connection t1 = openTx(TRANSACTION_SERIALIZABLE);
             Connection t2 = openTx(TRANSACTION_SERIALIZABLE)) {

            long sumSeenByT1 = scalar(t1, "select sum(balance) from linked_account");
            long sumSeenByT2 = scalar(t2, "select sum(balance) from linked_account");
            assertThat(sumSeenByT1).isEqualTo(200);
            assertThat(sumSeenByT2).isEqualTo(200);

            exec(t1, "update linked_account set balance = balance - 150 where name = 'A'");
            t1.commit();   // the first writer wins

            // T2's read of A now conflicts with T1's write of A (and vice-versa for B): SSI aborts T2.
            assertThatThrownBy(() -> {
                exec(t2, "update linked_account set balance = balance - 150 where name = 'B'");
                t2.commit();
            }).isInstanceOf(SQLException.class)
              .satisfies(e -> assertThat(((SQLException) e).getSQLState())
                      .as("Postgres serialization_failure SQLState")
                      .isEqualTo("40001"));

            System.out.println("[write-skew @SERIALIZABLE] second commit rejected with SQLState 40001 — "
                    + "the application would retry the transaction");
            t2.rollback();
        }

        long finalSum = combinedBalance();
        System.out.println("[write-skew @SERIALIZABLE] final combined balance = " + finalSum);
        assertThat(finalSum).isGreaterThanOrEqualTo(0);   // invariant held: only T1's debit applied (= 50)
    }
}
