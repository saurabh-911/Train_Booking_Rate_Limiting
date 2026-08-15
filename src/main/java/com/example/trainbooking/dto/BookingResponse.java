package com.example.trainbooking.dto;

public record BookingResponse(
        Long bookingId,
        String status,
        String message
) {
}
