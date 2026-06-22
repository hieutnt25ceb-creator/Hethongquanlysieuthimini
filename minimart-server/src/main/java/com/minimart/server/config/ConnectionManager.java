package com.minimart.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the HikariCP connection pool for the MySQL database.
 *
 * <p>Configuration is loaded from system properties (with defaults for local dev).
 * Override in production by passing JVM arguments:</p>
 * <pre>
 *   -Ddb.url=jdbc:mysql://host:3306/minimart_db
 *   -Ddb.username=root
 *   -Ddb.password=123456
 * </pre>
 */
public final class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();

        // ── Database connection settings ──────────────────────
        config.setJdbcUrl(System.getProperty("db.url",
                "jdbc:mysql://localhost:3306/minimart_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"));
        config.setUsername(System.getProperty("db.username", "root"));
        config.setPassword(System.getProperty("db.password", ""));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // ── Pool settings ─────────────────────────────────────
        config.setPoolName("MiniMartPool");
        config.setMaximumPoolSize(20);          // Max 20 concurrent DB connections
        config.setMinimumIdle(5);               // Keep at least 5 connections warm
        config.setConnectionTimeout(30_000);    // 30s — wait timeout for getting connection
        config.setIdleTimeout(600_000);         // 10m — max idle time before eviction
        config.setMaxLifetime(1_800_000);       // 30m — max connection lifetime

        // ── Connection validation ─────────────────────────────
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5_000);

        // ── Additional MySQL optimizations ────────────────────
        config.addDataSourceProperty("cachePrepStmts",          "true");
        config.addDataSourceProperty("prepStmtCacheSize",        "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");
        config.addDataSourceProperty("useServerPrepStmts",       "true");

        dataSource = new HikariDataSource(config);
        log.info("HikariCP connection pool initialized. Pool: {}", config.getPoolName());
    }

    private ConnectionManager() { /* utility class */ }

    /**
     * Returns a Connection from the pool.
     * Callers MUST close the connection in a try-with-resources block
     * so it is returned to the pool.
     *
     * @return an active {@link Connection}
     * @throws SQLException if no connection is available
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Gracefully shuts down the connection pool.
     * Call this once when the server is stopping.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP connection pool shut down.");
        }
    }
}
