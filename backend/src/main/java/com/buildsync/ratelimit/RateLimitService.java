package com.buildsync.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Core service coordinating rate limit checks, policy resolution,
 * and key anonymization across storage providers.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final RateLimitStorage storage;
    private final RateLimitProperties properties;

    public RateLimitService(RateLimitStorage storage, RateLimitProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    /**
     * Checks if a single action is permitted under the specified policy.
     */
    public RateLimitResult check(String rawIdentifier, RateLimitPolicy policy) {
        return check(rawIdentifier, policy, 1);
    }

    /**
     * Checks if the requested cost is permitted under the specified policy.
     */
    public RateLimitResult check(String rawIdentifier, RateLimitPolicy policy, int cost) {
        if (!properties.isEnabled()) {
            return RateLimitResult.permitted(policy.getDefaultCapacity(), policy.getDefaultCapacity(), 0);
        }

        long capacity = resolveCapacity(policy);
        long refillTokens = resolveRefillTokens(policy);
        Duration refillPeriod = resolveRefillPeriod(policy);

        String storageKey = buildStorageKey(rawIdentifier, policy);

        RateLimitResult result = storage.tryConsume(storageKey, cost, capacity, refillTokens, refillPeriod);

        if (result != null && !result.allowed()) {
            log.warn("Rate limit exceeded for policy '{}' on key prefix '{}...': retryAfter={}s, limit={}",
                    policy.name(),
                    storageKey.substring(0, Math.min(16, storageKey.length())),
                    result.retryAfterSeconds(),
                    result.limit());
        }

        return result;
    }

    /**
     * Clear all rate limit records from active storage.
     */
    public void clear() {
        storage.clear();
    }

    public RateLimitStorage getStorage() {
        return storage;
    }

    private long resolveCapacity(RateLimitPolicy policy) {
        RateLimitProperties.PolicyConfig config = properties.getPolicies().get(policy.getConfigKey());
        if (config != null && config.getCapacity() != null && config.getCapacity() > 0) {
            return config.getCapacity();
        }
        return policy.getDefaultCapacity();
    }

    private long resolveRefillTokens(RateLimitPolicy policy) {
        RateLimitProperties.PolicyConfig config = properties.getPolicies().get(policy.getConfigKey());
        if (config != null && config.getRefillTokens() != null && config.getRefillTokens() > 0) {
            return config.getRefillTokens();
        }
        return policy.getDefaultRefillTokens();
    }

    private Duration resolveRefillPeriod(RateLimitPolicy policy) {
        RateLimitProperties.PolicyConfig config = properties.getPolicies().get(policy.getConfigKey());
        if (config != null && config.getRefillPeriodSeconds() != null && config.getRefillPeriodSeconds() > 0) {
            return Duration.ofSeconds(config.getRefillPeriodSeconds());
        }
        return policy.getDefaultRefillPeriod();
    }

    private String buildStorageKey(String rawIdentifier, RateLimitPolicy policy) {
        String hashedIdentifier = sha256Hex(rawIdentifier != null ? rawIdentifier : "unknown");
        return "rl:" + policy.getConfigKey() + ":" + hashedIdentifier;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
