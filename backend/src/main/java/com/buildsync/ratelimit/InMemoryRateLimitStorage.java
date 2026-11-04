package com.buildsync.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, thread-safe in-memory token bucket rate limit storage.
 * Supports configurable Clock injection for deterministic unit and integration testing,
 * and maintains bounded memory via inactivity eviction.
 */
public class InMemoryRateLimitStorage implements RateLimitStorage {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimitStorage.class);
    private static final int MAX_BUCKETS_THRESHOLD = 50_000;
    private static final Duration DEFAULT_INACTIVITY_TTL = Duration.ofHours(1);

    private final Clock clock;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimitStorage() {
        this(Clock.systemUTC());
    }

    public InMemoryRateLimitStorage(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public RateLimitResult tryConsume(String key, int cost, long capacity, long refillTokens, Duration refillPeriod) {
        if (cost <= 0) {
            cost = 1;
        }

        // Trigger maintenance if bucket count grows large
        if (buckets.size() > MAX_BUCKETS_THRESHOLD) {
            evictInactiveBuckets(DEFAULT_INACTIVITY_TTL);
        }

        long nowMillis = clock.millis();
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, nowMillis));

        return bucket.consume(cost, capacity, refillTokens, refillPeriod, nowMillis);
    }

    /**
     * Evict buckets that have not been accessed within the given threshold.
     */
    public int evictInactiveBuckets(Duration maxInactivity) {
        long thresholdMillis = clock.millis() - maxInactivity.toMillis();
        int evictedCount = 0;

        for (Map.Entry<String, TokenBucket> entry : buckets.entrySet()) {
            if (entry.getValue().getLastAccessedMillis() < thresholdMillis) {
                if (buckets.remove(entry.getKey(), entry.getValue())) {
                    evictedCount++;
                }
            }
        }

        if (evictedCount > 0) {
            log.debug("Evicted {} inactive rate-limit buckets from in-memory cache", evictedCount);
        }
        return evictedCount;
    }

    @Override
    public void clear() {
        buckets.clear();
    }

    public int getActiveBucketCount() {
        return buckets.size();
    }

    /**
     * Internal thread-safe TokenBucket holding token balance and timestamps.
     */
    static class TokenBucket {
        private double tokens;
        private long lastRefillMillis;
        private volatile long lastAccessedMillis;

        TokenBucket(long capacity, long nowMillis) {
            this.tokens = capacity;
            this.lastRefillMillis = nowMillis;
            this.lastAccessedMillis = nowMillis;
        }

        synchronized RateLimitResult consume(int cost, long capacity, long refillTokens, Duration refillPeriod, long nowMillis) {
            this.lastAccessedMillis = nowMillis;

            long refillPeriodMillis = Math.max(1, refillPeriod.toMillis());
            double refillRatePerMilli = (double) refillTokens / (double) refillPeriodMillis;

            long elapsedMillis = Math.max(0, nowMillis - lastRefillMillis);
            if (elapsedMillis > 0) {
                double tokensToAdd = elapsedMillis * refillRatePerMilli;
                this.tokens = Math.min((double) capacity, this.tokens + tokensToAdd);
                this.lastRefillMillis = nowMillis;
            }

            double tokensNeededForReset = capacity - tokens;
            long resetSeconds = refillRatePerMilli > 0
                    ? (long) Math.ceil((tokensNeededForReset / (refillRatePerMilli * 1000.0)))
                    : 0;

            if (this.tokens >= cost) {
                this.tokens -= cost;
                long remaining = (long) Math.floor(this.tokens);
                return RateLimitResult.permitted(capacity, remaining, Math.max(0, resetSeconds));
            } else {
                double missingTokens = cost - this.tokens;
                long retryAfterSeconds = refillRatePerMilli > 0
                        ? (long) Math.ceil((missingTokens / (refillRatePerMilli * 1000.0)))
                        : 1;
                return RateLimitResult.rejected(capacity, Math.max(1, resetSeconds), Math.max(1, retryAfterSeconds));
            }
        }

        long getLastAccessedMillis() {
            return lastAccessedMillis;
        }
    }
}

