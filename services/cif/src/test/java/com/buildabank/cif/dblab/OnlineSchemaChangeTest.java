// services/cif/src/test/java/com/buildabank/cif/dblab/OnlineSchemaChangeTest.java
package com.buildabank.cif.dblab;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

/**
 * <strong>Zero-downtime (online) schema change.</strong> The two executable nuggets behind the
 * expand-contract pattern you'll use for real in Step 12:
 *
 * <ul>
 *   <li>{@code CREATE INDEX CONCURRENTLY} builds an index <em>without</em> a long table-blocking lock — but
 *       it cannot run inside a transaction block (so Flyway must run it outside one). We prove both the
 *       failure-in-a-transaction and the success-in-autocommit.</li>
 *   <li>Adding a column with a <em>constant</em> default is metadata-only since Postgres 11 — instant even on
 *       a large table, no full rewrite, no exclusive lock held for the whole scan.</li>
 * </ul>
 */
class OnlineSchemaChangeTest extends DbLab {

    @Test
    void createIndexConcurrentlyCannotRunInsideATransaction() throws Exception {
        try (Connection tx = openTx(TRANSACTION_READ_COMMITTED)) {   // autocommit OFF → we are "in a transaction"
            exec(tx, "drop table if exists osc");
            exec(tx, "create table osc (id int)");

            // CREATE INDEX CONCURRENTLY manages its own commits, so it refuses to run inside an open txn.
            assertThatThrownBy(() -> exec(tx, "create index concurrently idx_osc on osc (id)"))
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> assertThat(((SQLException) e).getSQLState())
                            .as("active_sql_transaction")
                            .isEqualTo("25001"));

            System.out.println("[online-ddl] CREATE INDEX CONCURRENTLY in a txn → SQLState 25001 (as expected)");
            tx.rollback();
        }
    }

    @Test
    void createIndexConcurrentlyAndFastDefaultWorkInAutocommit() throws Exception {
        try (Connection c = openAuto()) {                            // autocommit ON → each statement is its own txn
            exec(c, "drop table if exists osc2");
            exec(c, "create table osc2 (id int)");
            exec(c, "insert into osc2 select generate_series(1, 1000)");

            // Builds online — readers and writers are NOT blocked for the duration (unlike a plain CREATE INDEX).
            exec(c, "create index concurrently idx_osc2 on osc2 (id)");
            long indexes = scalar(c, "select count(*) from pg_indexes where indexname = 'idx_osc2'");
            assertThat(indexes).isEqualTo(1);
            System.out.println("[online-ddl] CREATE INDEX CONCURRENTLY in autocommit → index built online");

            // Adding a column with a CONSTANT default is metadata-only since PG 11 (no table rewrite).
            // All 1,000 existing rows logically get 'NEW' without scanning/rewriting the heap.
            exec(c, "alter table osc2 add column status text not null default 'NEW'");
            long withStatus = scalar(c, "select count(*) from osc2 where status = 'NEW'");
            assertThat(withStatus).isEqualTo(1000);
            System.out.println("[online-ddl] ADD COLUMN ... DEFAULT 'NEW' → 1000 rows backfilled (metadata-only)");
        }
    }
}
