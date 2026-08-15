package com.example.trainbooking.exception;

public class RateLimitException extends ApiException {
    public RateLimitException(String message) {
        super(message);
    }
}
