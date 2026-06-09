// services/cif/src/test/java/com/buildabank/cif/dblab/ConnectionPoolTest.java
package com.buildabank.cif.dblab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * <strong>HikariCP connection-pool internals.</strong> Opening a real database connection is expensive
 * (TCP + TLS + Postgres backend fork + auth), so production apps keep a small fixed <em>pool</em> of live
 * connections and lend them out. The pool is a turnstile: borrow → use → return. This lab builds a pool of
 * size <strong>2</strong> with a <strong>500&nbsp;ms</strong> borrow timeout and watches what happens when a
 * third caller asks for a connection while both are in use — the exact shape of a production "pool
 * exhaustion" incident.
 */
class ConnectionPoolTest extends DbLab {

    @Test
    void poolSaturationTimesOut_thenRecoversWhenAConnectionIsReturned() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(POSTGRES.getJdbcUrl());
        cfg.setUsername(POSTGRES.getUsername());
        cfg.setPassword(POSTGRES.getPassword());
        cfg.setMaximumPoolSize(2);          // only TWO connections may exist at once
        cfg.setConnectionTimeout(500);      // a borrower waits at most 500 ms, then gives up
        cfg.setPoolName("lab-pool");

        try (HikariDataSource pool = new HikariDataSource(cfg)) {
            HikariPoolMXBean mx = pool.getHikariPoolMXBean();

            // Borrow both connections and HOLD them (simulating two slow requests).
            Connection a = pool.getConnection();
            Connection b = pool.getConnection();

            System.out.println("[pool] after borrowing 2 → active=" + mx.getActiveConnections()
                    + " idle=" + mx.getIdleConnections() + " total=" + mx.getTotalConnections());
            assertThat(mx.getActiveConnections()).isEqualTo(2);
            assertThat(mx.getIdleConnections()).isZero();

            // A third borrow finds the pool saturated: it waits ~500 ms, then throws.
            long startNanos = System.nanoTime();
            assertThatThrownBy(pool::getConnection)
                    .isInstanceOf(SQLTransientConnectionException.class)
                    .hasMessageContaining("request timed out");
            long waitedMs = (System.nanoTime() - startNanos) / 1_000_000;
            // threadsAwaiting now reads 0: the borrower waited ~500 ms inside getConnection(), then gave up.
            System.out.println("[pool] 3rd borrow blocked ~" + waitedMs + " ms then timed out "
                    + "(threadsAwaiting now=" + mx.getThreadsAwaitingConnection() + ")");
            assertThat(waitedMs).isGreaterThanOrEqualTo(450);   // it really waited for the timeout

            // Return one connection → the next borrow succeeds immediately.
            a.close();
            try (Connection c = pool.getConnection()) {
                assertThat(c.isValid(1)).isTrue();
                System.out.println("[pool] after returning 1 → a fresh borrow succeeded; active="
                        + mx.getActiveConnections());
            }
            b.close();
        }
    }
}
