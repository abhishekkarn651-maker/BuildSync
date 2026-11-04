package com.buildsync.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitStorageTest {

    private MutableClock clock;
    private InMemoryRateLimitStorage storage;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-03T12:00:00Z"));
        storage = new InMemoryRateLimitStorage(clock);
    }

    @Test
    @DisplayName("Scenario 1: Token consumption within capacity succeeds with accurate remaining count")
    void testTokenConsumptionWithinCapacity() {
        String key = "user-1";
        RateLimitResult r1 = storage.tryConsume(key, 1, 5, 5, Duration.ofSeconds(60));
        assertThat(r1.allowed()).isTrue();
        assertThat(r1.remaining()).isEqualTo(4);
        assertThat(r1.limit()).isEqualTo(5);

        RateLimitResult r2 = storage.tryConsume(key, 2, 5, 5, Duration.ofSeconds(60));
        assertThat(r2.allowed()).isTrue();
        assertThat(r2.remaining()).isEqualTo(2);
    }

    @Test
    @DisplayName("Scenario 2: Token exhaustion rejects subsequent requests and reports retryAfter")
    void testTokenExhaustionRejection() {
        String key = "user-2";
        // Consume all 3 available tokens
        for (int i = 0; i < 3; i++) {
            RateLimitResult r = storage.tryConsume(key, 1, 3, 3, Duration.ofSeconds(60));
            assertThat(r.allowed()).isTrue();
        }

        // 4th request must be rejected
        RateLimitResult rejected = storage.tryConsume(key, 1, 3, 3, Duration.ofSeconds(60));
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.remaining()).isZero();
        assertThat(rejected.retryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Scenario 3: Deterministic Clock advancement replenishes tokens accurately")
    void testClockAdvancementRefill() {
        String key = "user-3";
        // 10 tokens per 60 seconds (1 token every 6 seconds)
        RateLimitResult r1 = storage.tryConsume(key, 10, 10, 10, Duration.ofSeconds(60));
        assertThat(r1.allowed()).isTrue();
        assertThat(r1.remaining()).isZero();

        // Immediately after, rejected
        RateLimitResult r2 = storage.tryConsume(key, 1, 10, 10, Duration.ofSeconds(60));
        assertThat(r2.allowed()).isFalse();

        // Advance clock by 18 seconds -> should replenish 3 tokens
        clock.advance(Duration.ofSeconds(18));

        RateLimitResult r3 = storage.tryConsume(key, 1, 10, 10, Duration.ofSeconds(60));
        assertThat(r3.allowed()).isTrue();
        assertThat(r3.remaining()).isEqualTo(2);
    }

    @Test
    @DisplayName("Scenario 4: Tokens do not exceed bucket maximum capacity on prolonged idle time")
    void testRefillCapAtMaxCapacity() {
        String key = "user-4";
        storage.tryConsume(key, 2, 5, 5, Duration.ofSeconds(60));

        // Advance time by 1 day
        clock.advance(Duration.ofDays(1));

        RateLimitResult r = storage.tryConsume(key, 1, 5, 5, Duration.ofSeconds(60));
        assertThat(r.allowed()).isTrue();
        // Should cap at 5, minus 1 consumed = 4
        assertThat(r.remaining()).isEqualTo(4);
    }

    @Test
    @DisplayName("Scenario 5: Multi-token cost consumption deducts full requested amount")
    void testMultiTokenCostConsumption() {
        String key = "user-5";
        RateLimitResult r = storage.tryConsume(key, 5, 10, 10, Duration.ofSeconds(60));
        assertThat(r.allowed()).isTrue();
        assertThat(r.remaining()).isEqualTo(5);

        // Attempting to consume 6 when 5 remaining should fail
        RateLimitResult r2 = storage.tryConsume(key, 6, 10, 10, Duration.ofSeconds(60));
        assertThat(r2.allowed()).isFalse();
        assertThat(r2.remaining()).isZero();
    }

    @Test
    @DisplayName("Scenario 6: Inactive bucket eviction successfully purges stale entries")
    void testInactiveBucketEviction() {
        storage.tryConsume("user-stale-1", 1, 10, 10, Duration.ofSeconds(60));
        storage.tryConsume("user-stale-2", 1, 10, 10, Duration.ofSeconds(60));
        assertThat(storage.getActiveBucketCount()).isEqualTo(2);

        // Advance clock by 30 minutes (less than 1 hour TTL)
        clock.advance(Duration.ofMinutes(30));
        int evictedEarly = storage.evictInactiveBuckets(Duration.ofHours(1));
        assertThat(evictedEarly).isZero();
        assertThat(storage.getActiveBucketCount()).isEqualTo(2);

        // Advance clock past 1 hour
        clock.advance(Duration.ofMinutes(35));
        int evictedLate = storage.evictInactiveBuckets(Duration.ofHours(1));
        assertThat(evictedLate).isEqualTo(2);
        assertThat(storage.getActiveBucketCount()).isZero();
    }

    @Test
    @DisplayName("Scenario 7: Distinct client keys operate in isolated independent buckets")
    void testIsolatedBucketsPerKey() {
        storage.tryConsume("key-A", 5, 5, 5, Duration.ofSeconds(60));
        RateLimitResult rejectA = storage.tryConsume("key-A", 1, 5, 5, Duration.ofSeconds(60));
        assertThat(rejectA.allowed()).isFalse();

        // key-B should still have full capacity
        RateLimitResult permitB = storage.tryConsume("key-B", 1, 5, 5, Duration.ofSeconds(60));
        assertThat(permitB.allowed()).isTrue();
        assertThat(permitB.remaining()).isEqualTo(4);
    }

    @Test
    @DisplayName("Scenario 8: Concurrent requests maintain thread-safety without race conditions")
    void testConcurrencyThreadSafety() throws InterruptedException {
        String key = "concurrent-user";
        int totalThreads = 20;
        int capacity = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RateLimitResult res = storage.tryConsume(key, 1, capacity, capacity, Duration.ofSeconds(60));
                    if (res.allowed()) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(capacity);
        assertThat(rejectedCount.get()).isEqualTo(totalThreads - capacity);
    }

    private static class MutableClock extends Clock {
        private Instant current;
        private final ZoneId zone;

        public MutableClock(Instant start) {
            this.current = start;
            this.zone = ZoneOffset.UTC;
        }

        public synchronized void advance(Duration duration) {
            this.current = this.current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current);
        }

        @Override
        public synchronized Instant instant() {
            return current;
        }

        @Override
        public synchronized long millis() {
            return current.toEpochMilli();
        }
    }
}

