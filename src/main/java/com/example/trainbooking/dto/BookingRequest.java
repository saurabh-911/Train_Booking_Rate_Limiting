package com.example.trainbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotBlank String userId,
        @NotNull Long seatId,
        @NotBlank String trainId,
        @NotBlank String idempotencyKey,
        String queueToken,
        String userTier
) {
}
