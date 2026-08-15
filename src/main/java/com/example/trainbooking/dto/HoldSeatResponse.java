package com.example.trainbooking.dto;

import java.time.Instant;

public record HoldSeatResponse(
        Long seatId,
        String status,
        Instant holdExpiresAt,
        String message
) {
}
