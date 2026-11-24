package com.example.hotel.model;

import java.io.Serializable;

/** A detached file example: loading it never updates SQLite. */
public final class ReservationSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String guestName;
    private final String phone;
    private final String roomNumber;
    private final String checkIn;
    private final String checkOut;
    private final long totalRupees;
    private final String status;

    public ReservationSnapshot(Reservation reservation) {
        id = reservation.getId();
        guestName = reservation.getGuest().getName();
        phone = reservation.getGuest().getPhone();
        roomNumber = reservation.getRoom().getNumber();
        checkIn = reservation.getCheckIn().toString();
        checkOut = reservation.getCheckOut().toString();
        totalRupees = reservation.getTotalRupees();
        status = reservation.getStatus().name();
    }

    public String display() {
        return "Reservation #" + id + "\nGuest: " + guestName + "\nPhone: " + phone
                + "\nRoom: " + roomNumber + "\nCheck-in: " + checkIn
                + "\nCheck-out: " + checkOut + "\nTotal: ₹" + totalRupees
                + "\nStatus: " + status + "\n";
    }
}
