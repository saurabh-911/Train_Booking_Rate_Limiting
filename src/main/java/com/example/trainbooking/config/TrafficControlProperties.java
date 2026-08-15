package com.example.trainbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "traffic-control")
public record TrafficControlProperties(
        Layer1 layer1,
        Queue queue,
        Adaptive adaptive
) {
    public record Layer1(int capacity, int refillTokens, int refillSeconds) {}
    public record Queue(int releasesPerSecond, int maxQueueSize, int avgServiceSeconds) {}
    public record Adaptive(int minLimit, int maxLimit, int increaseBy, double decreaseFactor, long highLatencyMs) {}
}
