package com.example.hotel.model;

public abstract class Room {
    private final long id;
    private final String number;
    private final int capacity;
    private final int nightlyPriceRupees;

    protected Room(long id, String number, int capacity, int nightlyPriceRupees) {
        if (id <= 0 || number == null || number.isBlank() || capacity <= 0 || nightlyPriceRupees < 0) {
            throw new IllegalArgumentException("Invalid room details");
        }
        this.id = id;
        this.number = number;
        this.capacity = capacity;
        this.nightlyPriceRupees = nightlyPriceRupees;
    }

    public long getId() { return id; }
    public String getNumber() { return number; }
    public int getCapacity() { return capacity; }
    public int getNightlyPriceRupees() { return nightlyPriceRupees; }
    public abstract String description();

    @Override
    public String toString() {
        return number + " — " + description();
    }
}
