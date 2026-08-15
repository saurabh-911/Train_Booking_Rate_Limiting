package com.example.trainbooking.kafka;

import java.time.Instant;

public record BookingConfirmedEvent(
        Long bookingId,
        String userId,
        Long seatId,
        String trainId,
        Instant bookedAt
) {
}
