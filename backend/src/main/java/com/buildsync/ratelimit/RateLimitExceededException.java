package com.buildsync.ratelimit;

/**
 * Exception thrown when a client exceeds the allocated rate limit quota.
 */
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitResult result;
    private final RateLimitPolicy policy;

    public RateLimitExceededException(RateLimitResult result, RateLimitPolicy policy) {
        super("Too many requests. Please try again later.");
        this.result = result;
        this.policy = policy;
    }

    public RateLimitExceededException(String message, RateLimitResult result, RateLimitPolicy policy) {
        super(message);
        this.result = result;
        this.policy = policy;
    }

    public RateLimitResult getResult() {
        return result;
    }

    public RateLimitPolicy getPolicy() {
        return policy;
    }
}

