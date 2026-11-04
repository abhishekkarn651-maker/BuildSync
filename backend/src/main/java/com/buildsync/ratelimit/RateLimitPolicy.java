package com.buildsync.ratelimit;

import java.time.Duration;

/**
 * Enumeration of the 14 rate-limiting policies in BuildSync with their default limits.
 */
public enum RateLimitPolicy {

    GENERAL_AUTHENTICATED(120, 120, Duration.ofSeconds(60)),
    GENERAL_ANONYMOUS(60, 60, Duration.ofSeconds(60)),
    AUTH_LOGIN(10, 10, Duration.ofSeconds(300)),
    AUTH_REGISTER(5, 5, Duration.ofSeconds(3600)),
    AUTH_REFRESH(30, 30, Duration.ofSeconds(900)),
    SEARCH_DEVELOPERS(60, 60, Duration.ofSeconds(60)),
    SEARCH_PROJECTS(60, 60, Duration.ofSeconds(60)),
    AVATAR_UPLOAD(10, 10, Duration.ofSeconds(3600)),
    COLLABORATION_ACTIONS(20, 20, Duration.ofSeconds(60)),
    DIRECT_MESSAGING(30, 30, Duration.ofSeconds(60)),
    PROJECT_GROUP_MESSAGING(30, 30, Duration.ofSeconds(60)),
    WEBSOCKET_CONNECT(10, 10, Duration.ofSeconds(60)),
    TYPING_EVENTS(60, 60, Duration.ofSeconds(60)),
    GITHUB_SYNC(5, 5, Duration.ofSeconds(3600));

    private final int defaultCapacity;
    private final int defaultRefillTokens;
    private final Duration defaultRefillPeriod;

    RateLimitPolicy(int defaultCapacity, int defaultRefillTokens, Duration defaultRefillPeriod) {
        this.defaultCapacity = defaultCapacity;
        this.defaultRefillTokens = defaultRefillTokens;
        this.defaultRefillPeriod = defaultRefillPeriod;
    }

    public int getDefaultCapacity() {
        return defaultCapacity;
    }

    public int getDefaultRefillTokens() {
        return defaultRefillTokens;
    }

    public Duration getDefaultRefillPeriod() {
        return defaultRefillPeriod;
    }

    public String getConfigKey() {
        return name().toLowerCase().replace('_', '-');
    }
}

