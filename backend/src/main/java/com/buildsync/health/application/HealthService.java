package com.buildsync.health.application;

import com.buildsync.health.api.HealthResponse;
import com.buildsync.health.infrastructure.DatabaseHealthChecker;
import org.springframework.stereotype.Service;

/**
 * Health application service coordinating system and database health state evaluations.
 */
@Service
public class HealthService {

    private final DatabaseHealthChecker databaseHealthChecker;

    public HealthService(DatabaseHealthChecker databaseHealthChecker) {
        this.databaseHealthChecker = databaseHealthChecker;
    }

    /**
     * Assesses overall system health and database availability.
     *
     * @return HealthResponse containing application status and database status.
     */
    public HealthResponse getSystemHealth() {
        boolean dbConnected = databaseHealthChecker.isDatabaseConnected();

        String databaseStatus = dbConnected ? "CONNECTED" : "DISCONNECTED";
        String applicationStatus = dbConnected ? "UP" : "DEGRADED";

        return HealthResponse.of(applicationStatus, databaseStatus);
    }
}
