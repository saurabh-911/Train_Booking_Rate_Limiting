package com.example.trainbooking.dto;

public record QueueStatusResponse(
        String userId,
        Long position,
        Long estimatedWaitSeconds,
        boolean admitted,
        String queueToken,
        Long tokenExpiresInSeconds
) {
}
