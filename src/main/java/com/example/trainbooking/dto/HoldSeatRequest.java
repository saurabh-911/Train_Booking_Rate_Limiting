package com.example.trainbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HoldSeatRequest(
        @NotBlank String userId,
        @NotNull Long seatId,
        @NotBlank String trainId
) {
}
