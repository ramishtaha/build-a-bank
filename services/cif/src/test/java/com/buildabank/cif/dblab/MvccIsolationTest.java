// services/cif/src/test/java/com/buildabank/cif/dblab/MvccIsolationTest.java
package com.buildabank.cif.dblab;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <strong>MVCC &amp; the SQL isolation anomalies</strong>, demonstrated by interleaving two real
 * transactions on Postgres. Each test resets a tiny {@code acct} table, so they are independent.
 *
 * <p>Postgres uses <em>Multi-Version Concurrency Control</em>: a writer never blocks a reader because each
 * transaction reads from a consistent <em>snapshot</em>. The level you pick decides how wide that snapshot
 * is — and therefore which anomalies you can still see.
 */
class MvccIsolationTest extends DbLab {

    @BeforeEach
    void resetTable() throws Exception {
        try (Connection c = openAuto()) {
            exec(c, "drop table if exists acct");
            exec(c, "create table acct (id int primary key, balance numeric not null)");
            exec(c, "insert into acct values (1, 100), (2, 100)");
        }
    }

    /**
     * <strong>Dirty read — impossible in Postgres.</strong> Even when we explicitly ask for READ
     * UNCOMMITTED, Postgres silently upgrades it to READ COMMITTED, so a reader can never see another
     * transaction's <em>uncommitted</em> write. We prove it: T2 writes-but-does-not-commit, T1 still reads
     * the old value.
     */
    @Test
    void dirtyReadIsImpossibleInPostgres() throws Exception {
        try (Connection t1 = openTx(TRANSACTION_READ_UNCOMMITTED);
             Connection t2 = openTx(TRANSACTION_READ_COMMITTED)) {

            exec(t2, "update acct set balance = 999 where id = 1");   // NOT committed

            long seenByT1 = scalar(t1, "select balance from acct where id = 1");
            System.out.println("[dirty-read] T1 (asked for READ UNCOMMITTED) sees balance = " + seenByT1);

            assertThat(seenByT1).isEqualTo(100);   // the old, committed value — no dirty read
            t1.rollback();
            t2.rollback();
        }
    }

    /**
     * <strong>Non-repeatable read — visible at READ COMMITTED.</strong> T1 reads a row twice; between the
     * reads T2 commits an update. At READ COMMITTED each statement gets a <em>fresh</em> snapshot, so the
     * second read sees the new value — the same query, two answers.
     */
    @Test
    void nonRepeatableReadHappensAtReadCommitted() throws Exception {
        try (Connection t1 = openTx(TRANSACTION_READ_COMMITTED)) {
            long first = scalar(t1, "select balance from acct where id = 1");

            try (Connection t2 = openTx(TRANSACTION_READ_COMMITTED)) {
                exec(t2, "update acct set balance = 200 where id = 1");
                t2.commit();
            }

            long second = scalar(t1, "select balance from acct where id = 1");
            System.out.println("[non-repeatable @RC] first=" + first + " second=" + second);

            assertThat(first).isEqualTo(100);
            assertThat(second).isEqualTo(200);   // changed under T1's feet
            t1.rollback();
        }
    }

    /**
     * <strong>Repeatable Read prevents it.</strong> Same interleaving, but T1 runs at REPEATABLE READ, which
     * pins one snapshot for the whole transaction — so both reads return the original value, even though T2
     * committed in between.
     */
    @Test
    void repeatableReadGivesAStableSnapshot() throws Exception {
        try (Connection t1 = openTx(TRANSACTION_REPEATABLE_READ)) {
            long first = scalar(t1, "select balance from acct where id = 1");   // takes the snapshot

            try (Connection t2 = openTx(TRANSACTION_READ_COMMITTED)) {
                exec(t2, "update acct set balance = 200 where id = 1");
                t2.commit();
            }

            long second = scalar(t1, "select balance from acct where id = 1");
            System.out.println("[repeatable-read] first=" + first + " second=" + second);

            assertThat(first).isEqualTo(100);
            assertThat(second).isEqualTo(100);   // T2's commit is invisible to T1's frozen snapshot
            t1.rollback();
        }
    }

    /**
     * <strong>Phantom rows.</strong> T1 counts rows matching a predicate; T2 inserts a new matching row and
     * commits. At READ COMMITTED the re-count grows (a phantom appears); at REPEATABLE READ the snapshot
     * hides it. (Postgres's snapshot model prevents phantoms at RR — stricter than the SQL standard
     * requires.)
     */
    @Test
    void phantomAppearsAtReadCommittedButNotAtRepeatableRead() throws Exception {
        // READ COMMITTED — phantom appears
        try (Connection t1 = openTx(TRANSACTION_READ_COMMITTED)) {
            long before = scalar(t1, "select count(*) from acct where balance >= 100");
            insertCommitted(3, 100);
            long after = scalar(t1, "select count(*) from acct where balance >= 100");
            System.out.println("[phantom @RC] before=" + before + " after=" + after);
            assertThat(before).isEqualTo(2);
            assertThat(after).isEqualTo(3);   // phantom row appeared
            t1.rollback();
        }

        resetTableQuietly();

        // REPEATABLE READ — no phantom
        try (Connection t1 = openTx(TRANSACTION_REPEATABLE_READ)) {
            long before = scalar(t1, "select count(*) from acct where balance >= 100");
            insertCommitted(4, 100);
            long after = scalar(t1, "select count(*) from acct where balance >= 100");
            System.out.println("[phantom @RR] before=" + before + " after=" + after);
            assertThat(before).isEqualTo(2);
            assertThat(after).isEqualTo(2);   // snapshot hides the new row
            t1.rollback();
        }
    }

    private void insertCommitted(int id, int balance) throws Exception {
        try (Connection t2 = openTx(TRANSACTION_READ_COMMITTED)) {
            exec(t2, "insert into acct values (" + id + ", " + balance + ")");
            t2.commit();
        }
    }

    private void resetTableQuietly() throws Exception {
        resetTable();
    }
}
