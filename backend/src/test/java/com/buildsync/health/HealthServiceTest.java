package com.buildsync.health;

import com.buildsync.health.api.HealthResponse;
import com.buildsync.health.application.HealthService;
import com.buildsync.health.infrastructure.DatabaseHealthChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private DatabaseHealthChecker databaseHealthChecker;

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new HealthService(databaseHealthChecker);
    }

    @Test
    @DisplayName("Should return UP and CONNECTED when database probe is healthy")
    void shouldReturnUpWhenDatabaseIsConnected() {
        when(databaseHealthChecker.isDatabaseConnected()).thenReturn(true);

        HealthResponse response = healthService.getSystemHealth();

        assertThat(response.application()).isEqualTo("BuildSync");
        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.database()).isEqualTo("CONNECTED");
    }

    @Test
    @DisplayName("Should return DEGRADED and DISCONNECTED when database probe fails")
    void shouldReturnDegradedWhenDatabaseIsDisconnected() {
        when(databaseHealthChecker.isDatabaseConnected()).thenReturn(false);

        HealthResponse response = healthService.getSystemHealth();

        assertThat(response.application()).isEqualTo("BuildSync");
        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.database()).isEqualTo("DISCONNECTED");
    }
}
