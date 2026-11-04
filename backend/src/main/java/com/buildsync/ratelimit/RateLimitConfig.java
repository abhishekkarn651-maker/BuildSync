package com.buildsync.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

/**
 * Spring configuration registering the appropriate RateLimitStorage implementation
 * based on application properties ('buildsync.rate-limit.storage').
 */
@Configuration
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "buildsync.rate-limit.storage", havingValue = "redis")
    public RateLimitStorage redisRateLimitStorage(
            StringRedisTemplate redisTemplate,
            Clock clock,
            RateLimitProperties properties) {
        log.info("Initializing Redis distributed rate limit storage engine");
        RedisRateLimitStorage storage = new RedisRateLimitStorage(redisTemplate, clock);
        if (properties.isEnabled()) {
            // Fail-fast on startup if Redis storage is configured but Redis is unreachable
            storage.validateConnection();
        }
        return storage;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(RateLimitStorage.class)
    public RateLimitStorage inMemoryRateLimitStorage(Clock clock) {
        log.info("Initializing In-Memory token bucket rate limit storage engine");
        return new InMemoryRateLimitStorage(clock);
    }
}

