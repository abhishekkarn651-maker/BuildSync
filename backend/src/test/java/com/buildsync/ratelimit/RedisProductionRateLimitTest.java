package com.buildsync.ratelimit;

import com.github.fppt.jedismock.RedisServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisProductionRateLimitTest {

    private static RedisServer redisServer;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisRateLimitStorage redisStorage;

    @BeforeAll
    static void startRedisServer() throws IOException {
        redisServer = RedisServer.newRedisServer();
        redisServer.start();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redisServer.getHost(), redisServer.getBindPort()
        );
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        redisStorage = new RedisRateLimitStorage(redisTemplate, Clock.systemUTC());
    }

    @AfterAll
    static void stopRedisServer() throws IOException {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @Test
    @DisplayName("Redis Test 1: Production startup validation succeeds when Redis responds to PING")
    void testProductionStartupValidationSuccess() {
        redisStorage.validateConnection();
    }

    @Test
    @DisplayName("Redis Test 2: Atomic token consumption executes over TCP and returns correct remaining quota")
    void testAtomicTokenConsumption() {
        String key = "rl:test:atomic-user";
        RateLimitResult r1 = redisStorage.tryConsume(key, 1, 5, 5, Duration.ofSeconds(60));

        assertThat(r1.allowed()).isTrue();
        assertThat(r1.limit()).isEqualTo(5);
        assertThat(r1.remaining()).isEqualTo(4);

        RateLimitResult r2 = redisStorage.tryConsume(key, 2, 5, 5, Duration.ofSeconds(60));
        assertThat(r2.allowed()).isTrue();
        assertThat(r2.remaining()).isEqualTo(2);
    }

    @Test
    @DisplayName("Redis Test 3: Token exhaustion rejects requests atomically via Lua script")
    void testTokenExhaustion() {
        String key = "rl:test:exhaust-user";
        // Consume all 3 tokens
        for (int i = 0; i < 3; i++) {
            RateLimitResult res = redisStorage.tryConsume(key, 1, 3, 3, Duration.ofSeconds(60));
            assertThat(res.allowed()).isTrue();
        }

        // 4th request must be rejected
        RateLimitResult rejected = redisStorage.tryConsume(key, 1, 3, 3, Duration.ofSeconds(60));
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.remaining()).isZero();
        assertThat(rejected.retryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Redis Test 4: Concurrent multi-threaded requests maintain atomic consistency without race conditions")
    void testConcurrentRequestsOverTcp() throws InterruptedException {
        String key = "rl:test:concurrent-user";
        int totalThreads = 20;
        int capacity = 8;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RateLimitResult res = redisStorage.tryConsume(key, 1, capacity, capacity, Duration.ofSeconds(60));
                    if (res.allowed()) {
                        successCount.incrementAndGet();
                    } else {
                        rejectCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(capacity);
        assertThat(rejectCount.get()).isEqualTo(totalThreads - capacity);
    }

    @Test
    @DisplayName("Redis Test 5: Key expiration (TTL) is automatically set by Lua script to prevent memory leaks")
    void testKeyExpirationTtl() {
        String key = "rl:test:expire-user";
        redisStorage.tryConsume(key, 1, 10, 10, Duration.ofSeconds(60));

        Long expireSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(expireSeconds).isNotNull();
        // TTL is configured as max(refillPeriod * 2, 3600s) = 3600s
        assertThat(expireSeconds).isGreaterThan(0);
    }

    @Test
    @DisplayName("Redis Test 6: Shared counters across separate application node client instances")
    void testSharedCountersAcrossNodes() {
        String key = "rl:test:shared-node-user";

        // Node A instance
        RedisRateLimitStorage nodeA = new RedisRateLimitStorage(redisTemplate, Clock.systemUTC());
        // Node B instance
        RedisRateLimitStorage nodeB = new RedisRateLimitStorage(redisTemplate, Clock.systemUTC());

        // Node A consumes 3 tokens of 5
        RateLimitResult resA = nodeA.tryConsume(key, 3, 5, 5, Duration.ofSeconds(60));
        assertThat(resA.allowed()).isTrue();
        assertThat(resA.remaining()).isEqualTo(2);

        // Node B immediately sees only 2 remaining tokens
        RateLimitResult resB = nodeB.tryConsume(key, 2, 5, 5, Duration.ofSeconds(60));
        assertThat(resB.allowed()).isTrue();
        assertThat(resB.remaining()).isZero();

        // Node A now attempts to consume 1 token and is rejected
        RateLimitResult resA2 = nodeA.tryConsume(key, 1, 5, 5, Duration.ofSeconds(60));
        assertThat(resA2.allowed()).isFalse();
    }

    @Test
    @DisplayName("Redis Test 7: Fail-Fast validation throws IllegalStateException if Redis is unavailable")
    void testProductionStartupValidationFailure() throws IOException {
        // Create an unreachable port
        RedisStandaloneConfiguration deadConfig = new RedisStandaloneConfiguration("127.0.0.1", 64999);
        LettuceConnectionFactory deadFactory = new LettuceConnectionFactory(deadConfig);
        deadFactory.afterPropertiesSet();
        StringRedisTemplate deadTemplate = new StringRedisTemplate(deadFactory);
        deadTemplate.afterPropertiesSet();

        RedisRateLimitStorage deadStorage = new RedisRateLimitStorage(deadTemplate, Clock.systemUTC());

        assertThatThrownBy(deadStorage::validateConnection)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis is unreachable");

        deadFactory.destroy();
    }

    @Test
    @DisplayName("Redis Test 8: Trusted proxy IP resolution integrates with Redis SHA-256 storage keys")
    void testTrustedProxyIpWithRedisStorage() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxyEnabled(true);
        ClientIpResolver resolver = new ClientIpResolver(props);
        RateLimitService service = new RateLimitService(redisStorage, props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.88, 10.0.0.1");

        String clientIp = resolver.resolveClientIp(request);
        assertThat(clientIp).isEqualTo("203.0.113.88");

        RateLimitResult result = service.check(clientIp, RateLimitPolicy.GENERAL_ANONYMOUS);
        assertThat(result.allowed()).isTrue();

        // Verify that the key in Redis is hashed with SHA-256 and does NOT contain raw IP
        var keys = redisTemplate.keys("rl:general-anonymous:*");
        assertThat(keys).isNotNull();
        boolean foundHashedKey = keys.stream().anyMatch(k -> !k.contains("203.0.113.88") && k.length() > 30);
        assertThat(foundHashedKey).isTrue();
    }
}

