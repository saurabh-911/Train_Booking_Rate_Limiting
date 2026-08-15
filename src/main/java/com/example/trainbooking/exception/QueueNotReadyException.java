package com.example.trainbooking.exception;

public class QueueNotReadyException extends ApiException {
    public QueueNotReadyException(String message) {
        super(message);
    }
}
