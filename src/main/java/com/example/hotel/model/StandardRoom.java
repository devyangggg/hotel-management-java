package com.example.hotel.model;

public final class StandardRoom extends Room {
    public StandardRoom(long id, String number, int capacity, int nightlyPriceRupees) {
        super(id, number, capacity, nightlyPriceRupees);
    }

    @Override
    public String description() {
        return "Standard — Wi-Fi";
    }
}
