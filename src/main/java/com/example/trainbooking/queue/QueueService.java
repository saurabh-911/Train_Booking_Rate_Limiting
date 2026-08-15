package com.example.trainbooking.queue;

import com.example.trainbooking.config.TrafficControlProperties;
import com.example.trainbooking.dto.QueueStatusResponse;
import com.example.trainbooking.exception.ApiException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QueueService {

    private static final String BOOKING_QUEUE = "booking_queue";
    private static final String ADMITTED_PREFIX = "queue:admitted:";
    private static final String TOKEN_PREFIX = "queue:token:";

    private final StringRedisTemplate redisTemplate;
    private final TrafficControlProperties properties;

    public QueueService(StringRedisTemplate redisTemplate, TrafficControlProperties properties, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        Gauge.builder("booking.queue.size", this, QueueService::getQueueSizeGauge).register(meterRegistry);
    }

    public void enqueueIfAbsent(String userId, String userTier) {
        Long size = redisTemplate.opsForZSet().size(BOOKING_QUEUE);
        if (size != null && size >= properties.queue().maxQueueSize()) {
            throw new ApiException("Virtual waiting room is full. Please retry in a few seconds.");
        }
        Double score = redisTemplate.opsForZSet().score(BOOKING_QUEUE, userId);
        if (score == null) {
            redisTemplate.opsForZSet().add(BOOKING_QUEUE, userId, calculateScore(userTier));
            log.debug("User {} added to queue tier={}", userId, userTier);
        }
    }

    public QueueStatusResponse getStatus(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(BOOKING_QUEUE, userId);
        boolean admitted = isAdmitted(userId);

        if (admitted) {
            String token = issueOrGetQueueToken(userId);
            return new QueueStatusResponse(userId, 0L, 0L, true, token, 30L);
        }
        if (rank == null) {
            return new QueueStatusResponse(userId, -1L, 0L, false, null, 0L);
        }

        long position = rank + 1;
        long releaseRate = Math.max(1, properties.queue().releasesPerSecond());
        long estimatedWait = Math.max(1, position / releaseRate) * properties.queue().avgServiceSeconds();
        return new QueueStatusResponse(userId, position, estimatedWait, false, null, 0L);
    }

    public boolean isAdmitted(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ADMITTED_PREFIX + userId));
    }

    public boolean validateQueueToken(String userId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String saved = redisTemplate.opsForValue().get(TOKEN_PREFIX + userId);
        return token.equals(saved);
    }

    public void consumeAdmission(String userId) {
        redisTemplate.delete(ADMITTED_PREFIX + userId);
        redisTemplate.delete(TOKEN_PREFIX + userId);
    }

    public void releaseUsers() {
        int batchSize = properties.queue().releasesPerSecond();
        Set<String> nextUsers = redisTemplate.opsForZSet().range(BOOKING_QUEUE, 0, batchSize - 1);
        if (nextUsers == null || nextUsers.isEmpty()) {
            return;
        }

        nextUsers.stream().filter(Objects::nonNull).forEach(userId -> {
            redisTemplate.opsForValue().set(ADMITTED_PREFIX + userId, "1", 60, TimeUnit.SECONDS);
            redisTemplate.opsForZSet().remove(BOOKING_QUEUE, userId);
        });
        log.info("Released {} users from virtual waiting room", nextUsers.size());
    }

    private String issueOrGetQueueToken(String userId) {
        String key = TOKEN_PREFIX + userId;
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
            return existing;
        }

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key, token, 30, TimeUnit.SECONDS);
        return token;
    }

    private double calculateScore(String userTier) {
        long now = Instant.now().toEpochMilli();
        long priorityWeightMs = "PREMIUM".equalsIgnoreCase(userTier) ? 120_000L : 0L;
        return now - priorityWeightMs;
    }

    private double getQueueSizeGauge() {
        Long size = redisTemplate.opsForZSet().size(BOOKING_QUEUE);
        return size == null ? 0 : size;
    }
}
