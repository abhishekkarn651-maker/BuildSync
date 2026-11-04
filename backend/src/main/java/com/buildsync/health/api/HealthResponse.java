package com.buildsync.health.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Health response DTO for the system monitoring endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String application,
        String status,
        String database
) {
    public static HealthResponse of(String status, String databaseStatus) {
        return new HealthResponse("BuildSync", status, databaseStatus);
    }
}
