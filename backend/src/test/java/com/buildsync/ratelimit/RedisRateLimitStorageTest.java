package com.buildsync.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitStorageTest {

    @Test
    @DisplayName("Scenario 13: Redis storage fails fast with IllegalStateException if connection check fails")
    void testFailFastWhenRedisUnreachable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Connection refused"));

        RedisRateLimitStorage storage = new RedisRateLimitStorage(redisTemplate, Clock.systemUTC());

        assertThatThrownBy(storage::validateConnection)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis is unreachable");
    }

    @Test
    @DisplayName("Scenario 14: RateLimitService anonymizes identifiers using SHA-256 in storage keys")
    void testSha256StorageKeyHashing() {
        RateLimitStorage storage = mock(RateLimitStorage.class);
        RateLimitProperties props = new RateLimitProperties();
        RateLimitService service = new RateLimitService(storage, props);

        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(storage.tryConsume(keyCaptor.capture(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(RateLimitResult.permitted(60, 59, 0));

        RateLimitResult result = service.check("secret-ip-or-user-uuid", RateLimitPolicy.GENERAL_ANONYMOUS);
        
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        org.assertj.core.api.Assertions.assertThat(result.allowed()).isTrue();
        
        String capturedKey = keyCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(capturedKey).startsWith("rl:general-anonymous:");
        // Sensitive raw identifier must NOT appear in the storage key
        org.assertj.core.api.Assertions.assertThat(capturedKey).doesNotContain("secret-ip-or-user-uuid");
        // Key suffix must be a 64-character SHA-256 hex string
        String hashPart = capturedKey.substring("rl:general-anonymous:".length());
        org.assertj.core.api.Assertions.assertThat(hashPart).hasSize(64);
    }
}
