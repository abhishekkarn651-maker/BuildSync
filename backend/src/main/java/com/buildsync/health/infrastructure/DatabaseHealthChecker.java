package com.buildsync.health.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Infrastructure probe that verifies actual database connectivity.
 * Executes a validation query against the configured DataSource with strict timeout
 * and error isolation, preventing sensitive details or credentials from leaking.
 */
@Component
public class DatabaseHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthChecker.class);
    private final DataSource dataSource;

    public DatabaseHealthChecker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Checks if the database is reachable and responsive.
     *
     * @return true if database connection and query succeeded, false otherwise.
     */
    public boolean isDatabaseConnected() {
        if (dataSource == null) {
            log.warn("Database check failed: DataSource is null");
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            if (conn.isClosed()) {
                return false;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(3);
                stmt.execute("SELECT 1");
                return true;
            }
        } catch (Exception ex) {
            log.warn("Database connectivity probe failed: {}", ex.getMessage());
            return false;
        }
    }
}
