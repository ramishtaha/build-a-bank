// services/cif/src/test/java/com/buildabank/cif/dblab/DbLab.java
package com.buildabank.cif.dblab;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared scaffolding for the Step-10 "relational databases up close" labs.
 *
 * <p>These labs talk to a <strong>real PostgreSQL</strong> via <strong>raw JDBC</strong> — deliberately no
 * Spring, no Hibernate. The whole point of this step is to get <em>close to the engine</em>: read query
 * plans, drive isolation levels by hand, watch MVCC snapshots, and saturate a connection pool. A
 * persistence framework would hide exactly the mechanics we want to see.
 *
 * <p>One container is started for the whole JVM (the Testcontainers <em>singleton-container</em> pattern)
 * and reused by every lab class — far cheaper than one container per class. The Testcontainers reaper
 * ("Ryuk") removes it when the JVM exits. The image is pinned (never {@code latest}).
 */
abstract class DbLab {

    /** Started once for the JVM, shared by every lab class. */
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    static {
        POSTGRES.start();
    }

    /** Open a connection at a chosen isolation level with autocommit OFF — we drive the transaction by hand. */
    static Connection openTx(int isolationLevel) throws SQLException {
        Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        c.setTransactionIsolation(isolationLevel);
        c.setAutoCommit(false);
        return c;
    }

    /** Open an autocommit connection (each statement commits immediately) — for DDL and seeding. */
    static Connection openAuto() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /** Run EXPLAIN (ANALYZE actually executes the query and reports real timings/buffers). */
    static String explain(Connection c, String sql, boolean analyze) throws SQLException {
        String prefix = analyze ? "EXPLAIN (ANALYZE, BUFFERS) " : "EXPLAIN ";
        StringBuilder plan = new StringBuilder();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(prefix + sql)) {
            while (rs.next()) {
                plan.append(rs.getString(1)).append('\n');
            }
        }
        return plan.toString();
    }

    /** Execute a statement (DDL or DML) ignoring any result set. */
    static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    /** Read a single numeric value (e.g. a COUNT or SUM). */
    static long scalar(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
