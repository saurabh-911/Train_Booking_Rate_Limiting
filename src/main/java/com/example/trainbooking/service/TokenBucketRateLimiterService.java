package com.example.trainbooking.service;

import com.example.trainbooking.config.TrafficControlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBucketRateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final TrafficControlProperties properties;

    public boolean tryConsume(String principalKey) {
        String redisKey = "rl:" + principalKey;
        Map<Object, Object> state = redisTemplate.opsForHash().entries(redisKey);

        double capacity = properties.layer1().capacity();
        double refillTokens = properties.layer1().refillTokens();
        double refillSeconds = properties.layer1().refillSeconds();

        double tokens = state.containsKey("tokens") ? Double.parseDouble(state.get("tokens").toString()) : capacity;
        long lastRefill = state.containsKey("lastRefill") ? Long.parseLong(state.get("lastRefill").toString()) : Instant.now().getEpochSecond();

        long now = Instant.now().getEpochSecond();
        long elapsed = Math.max(0, now - lastRefill);
        double refillRatePerSecond = refillTokens / refillSeconds;
        tokens = Math.min(capacity, tokens + (elapsed * refillRatePerSecond));

        boolean allowed = tokens >= 1;
        if (allowed) {
            tokens -= 1;
        }

        redisTemplate.opsForHash().put(redisKey, "tokens", String.valueOf(tokens));
        redisTemplate.opsForHash().put(redisKey, "lastRefill", String.valueOf(now));
        redisTemplate.expire(redisKey, 15, TimeUnit.MINUTES);

        return allowed;
    }
}
