package com.buildsync.ratelimit;

/**
 * Result of a rate-limit check containing consumption status and quota metadata.
 */
public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long resetSeconds,
        long retryAfterSeconds
) {
    public static RateLimitResult permitted(long limit, long remaining, long resetSeconds) {
        return new RateLimitResult(true, limit, remaining, resetSeconds, 0);
    }

    public static RateLimitResult rejected(long limit, long resetSeconds, long retryAfterSeconds) {
        return new RateLimitResult(false, limit, 0, resetSeconds, Math.max(1, retryAfterSeconds));
    }
}

