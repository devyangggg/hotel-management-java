package com.example.hotel.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Reservation {
    private final long id;
    private final Guest guest;
    private final Room room;
    private final int guests;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final long totalRupees;
    private final ReservationStatus status;

    public Reservation(long id, Guest guest, Room room, int guests, LocalDate checkIn,
                       LocalDate checkOut, long totalRupees, ReservationStatus status) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid reservation details");
        }
        if (guests <= 0) {
            throw new IllegalArgumentException("Invalid reservation details");
        }
        Objects.requireNonNull(room, "room");
        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("Invalid reservation details");
        }
        if (totalRupees < 0) {
            throw new IllegalArgumentException("Invalid reservation details");
        }
        Objects.requireNonNull(checkOut, "checkOut");
        Objects.requireNonNull(checkIn, "checkIn");
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Invalid reservation details");
        }
        this.id = id;
        this.guest = Objects.requireNonNull(guest, "guest");
        this.room = room;
        this.guests = guests;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalRupees = totalRupees;
        this.status = Objects.requireNonNull(status, "status");
    }

    public long getId() { return id; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public int getGuests() { return guests; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public long getTotalRupees() { return totalRupees; }
    public ReservationStatus getStatus() { return status; }
    public long nights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
}
