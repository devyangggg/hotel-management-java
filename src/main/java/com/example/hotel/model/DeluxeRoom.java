package com.example.hotel.model;

public final class DeluxeRoom extends Room {
    public DeluxeRoom(long id, String number, int capacity, int nightlyPriceRupees) {
        super(id, number, capacity, nightlyPriceRupees);
    }

    @Override
    public String description() {
        return "Deluxe — Wi-Fi and breakfast";
    }
}
