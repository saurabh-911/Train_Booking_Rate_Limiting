package com.example.trainbooking.exception;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
