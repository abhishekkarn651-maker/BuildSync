package com.buildsync.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for BuildSync Rate Limiting.
 */
@Configuration
@ConfigurationProperties(prefix = "buildsync.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String storage = "memory"; // memory | redis
    private boolean trustedProxyEnabled = false;
    private Redis redis = new Redis();
    private Map<String, PolicyConfig> policies = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public boolean isTrustedProxyEnabled() {
        return trustedProxyEnabled;
    }

    public void setTrustedProxyEnabled(boolean trustedProxyEnabled) {
        this.trustedProxyEnabled = trustedProxyEnabled;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Map<String, PolicyConfig> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, PolicyConfig> policies) {
        this.policies = policies;
    }

    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int timeoutMs = 2000;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class PolicyConfig {
        private Integer capacity;
        private Integer refillTokens;
        private Long refillPeriodSeconds;

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }

        public Integer getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(Integer refillTokens) {
            this.refillTokens = refillTokens;
        }

        public Long getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }

        public void setRefillPeriodSeconds(Long refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}

