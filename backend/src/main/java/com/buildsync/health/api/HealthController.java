package com.buildsync.health.api;

import com.buildsync.health.application.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health-check REST controller providing system and database connectivity status.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System health and database connectivity endpoints")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    @Operation(
            summary = "Get System Health Status",
            description = "Returns the operational status of the BuildSync application and real PostgreSQL database connectivity."
    )
    @ApiResponse(responseCode = "200", description = "Health status retrieved successfully")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse health = healthService.getSystemHealth();
        return ResponseEntity.ok(health);
    }
}
