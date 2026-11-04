package com.buildsync.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Distributed Redis rate-limiting storage utilizing an atomic Lua script
 * for concurrency-safe token bucket operations across application nodes.
 */
public class RedisRateLimitStorage implements RateLimitStorage {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStorage.class);

    private static final String LUA_TOKEN_BUCKET_SCRIPT =
            "local key = KEYS[1]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local refillTokens = tonumber(ARGV[2])\n" +
            "local refillPeriod = tonumber(ARGV[3])\n" +
            "local cost = tonumber(ARGV[4])\n" +
            "local now = tonumber(ARGV[5])\n" +
            "\n" +
            "local rate = refillTokens / refillPeriod\n" +
            "local data = redis.call('HMGET', key, 'tokens', 'last_updated')\n" +
            "local tokens = tonumber(data[1])\n" +
            "local last_updated = tonumber(data[2])\n" +
            "\n" +
            "if not tokens or not last_updated then\n" +
            "    tokens = capacity\n" +
            "    last_updated = now\n" +
            "else\n" +
            "    local elapsed = math.max(0, now - last_updated)\n" +
            "    tokens = math.min(capacity, tokens + (elapsed * rate))\n" +
            "    last_updated = now\n" +
            "end\n" +
            "\n" +
            "local allowed = 0\n" +
            "local retry_after = 0\n" +
            "local remaining = math.floor(tokens)\n" +
            "\n" +
            "if tokens >= cost then\n" +
            "    allowed = 1\n" +
            "    tokens = tokens - cost\n" +
            "    remaining = math.floor(tokens)\n" +
            "else\n" +
            "    allowed = 0\n" +
            "    local missing = cost - tokens\n" +
            "    retry_after = math.ceil(missing / rate)\n" +
            "end\n" +
            "\n" +
            "local tokens_needed = capacity - tokens\n" +
            "local reset_seconds = math.ceil(tokens_needed / rate)\n" +
            "\n" +
            "redis.call('HMSET', key, 'tokens', tokens, 'last_updated', last_updated)\n" +
            "local ttl = math.max(refillPeriod * 2, 3600)\n" +
            "redis.call('EXPIRE', key, ttl)\n" +
            "\n" +
            "return { allowed, capacity, remaining, reset_seconds, retry_after }\n";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> redisScript;
    private final Clock clock;

    public RedisRateLimitStorage(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    public RedisRateLimitStorage(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.redisScript = new DefaultRedisScript<>(LUA_TOKEN_BUCKET_SCRIPT, List.class);
    }

    /**
     * Validates that the configured Redis instance is accessible, failing fast on error.
     */
    public void validateConnection() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            throw new IllegalStateException("RedisConnectionFactory is null. Cannot connect to Redis.");
        }
        try (RedisConnection connection = factory.getConnection()) {
            String ping = connection.ping();
            log.info("Successfully connected to Redis for rate limiting (ping response: {})", ping);
        } catch (Exception ex) {
            log.error("Failed to connect to Redis for distributed rate limiting: {}", ex.getMessage());
            throw new IllegalStateException("BuildSync Rate Limiter is configured for 'redis' storage, but Redis is unreachable: " + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitResult tryConsume(String key, int cost, long capacity, long refillTokens, Duration refillPeriod) {
        if (cost <= 0) {
            cost = 1;
        }

        long refillPeriodSeconds = Math.max(1, refillPeriod.toSeconds());
        long nowSeconds = clock.instant().getEpochSecond();

        List<Object> args = List.of(
                String.valueOf(capacity),
                String.valueOf(refillTokens),
                String.valueOf(refillPeriodSeconds),
                String.valueOf(cost),
                String.valueOf(nowSeconds)
        );

        try {
            List<Long> result = redisTemplate.execute(redisScript, Collections.singletonList(key), args.toArray());
            if (result == null || result.size() < 5) {
                log.warn("Unexpected Redis response evaluating rate limit on key '{}', falling back to permit", key);
                return RateLimitResult.permitted(capacity, capacity - cost, 0);
            }

            boolean allowed = ((Number) result.get(0)).intValue() == 1;
            long limit = ((Number) result.get(1)).longValue();
            long remaining = ((Number) result.get(2)).longValue();
            long resetSeconds = ((Number) result.get(3)).longValue();
            long retryAfterSeconds = ((Number) result.get(4)).longValue();

            if (allowed) {
                return RateLimitResult.permitted(limit, remaining, resetSeconds);
            } else {
                return RateLimitResult.rejected(limit, resetSeconds, retryAfterSeconds);
            }
        } catch (Exception ex) {
            log.error("Error executing Redis rate limit script for key '{}': {}", key, ex.getMessage(), ex);
            throw new IllegalStateException("Redis rate limiting operation failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void clear() {
        try {
            var keys = redisTemplate.keys("rl:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Failed to clear Redis rate limit keys: {}", ex.getMessage());
        }
    }
}

