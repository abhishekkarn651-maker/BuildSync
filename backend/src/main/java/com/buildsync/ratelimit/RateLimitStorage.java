package com.buildsync.ratelimit;

import java.time.Duration;

/**
 * Storage SPI for rate limit quota management and token bucket calculations.
 */
public interface RateLimitStorage {

    /**
     * Attempts to consume the specified number of tokens from the identified bucket.
     *
     * @param key unique bucket key (namespaced)
     * @param cost tokens requested to consume
     * @param capacity maximum token capacity of the bucket
     * @param refillTokens tokens replenished per refill period
     * @param refillPeriod duration of the refill cycle
     * @return result indicating if the request was allowed, with remaining tokens and reset timing
     */
    RateLimitResult tryConsume(String key, int cost, long capacity, long refillTokens, Duration refillPeriod);

    /**
     * Clear all cached buckets (for testing and administrative maintenance).
     */
    void clear();
}

