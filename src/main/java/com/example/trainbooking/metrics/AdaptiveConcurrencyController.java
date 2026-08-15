package com.example.trainbooking.metrics;

import com.example.trainbooking.config.TrafficControlProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AdaptiveConcurrencyController {

    private final TrafficControlProperties properties;
    private final AtomicInteger concurrencyLimit;
    @Getter
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public AdaptiveConcurrencyController(TrafficControlProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.concurrencyLimit = new AtomicInteger(properties.adaptive().minLimit());
        Gauge.builder("booking.adaptive.limit", concurrencyLimit, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("booking.inflight", inFlight, AtomicInteger::get).register(meterRegistry);
    }

    public boolean tryAcquire() {
        int currentInFlight = inFlight.incrementAndGet();
        if (currentInFlight > concurrencyLimit.get()) {
            inFlight.decrementAndGet();
            return false;
        }
        return true;
    }

    public void releaseAndRecord(long latencyMs, boolean success) {
        inFlight.decrementAndGet();
        int current = concurrencyLimit.get();
        if (!success || latencyMs > properties.adaptive().highLatencyMs()) {
            int decreased = Math.max(properties.adaptive().minLimit(), (int) Math.floor(current * properties.adaptive().decreaseFactor()));
            concurrencyLimit.set(decreased);
            return;
        }
        int increased = Math.min(properties.adaptive().maxLimit(), current + properties.adaptive().increaseBy());
        concurrencyLimit.set(increased);
    }

    public int getConcurrencyLimit() {
        return concurrencyLimit.get();
    }
}
