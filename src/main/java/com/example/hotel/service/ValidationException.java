package com.example.hotel.service;

public final class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
    public ValidationException(String message, Throwable cause) { super(message, cause); }
}
