package com.example.hotel.service;

public final class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String message) { super(message); }
}
