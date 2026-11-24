package com.example.hotel.service;

import com.example.hotel.dao.ReservationDao;
import com.example.hotel.model.Guest;
import com.example.hotel.model.Reservation;
import com.example.hotel.model.ReservationStatus;
import com.example.hotel.model.Room;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public final class ReservationService {
    private final ReservationDao dao;
    private final Clock clock;

    public ReservationService(ReservationDao dao, Clock clock) {
        this.dao = Objects.requireNonNull(dao, "dao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<Room> rooms() throws SQLException {
        return dao.listRooms();
    }

    public List<Room> searchAvailable(LocalDate checkIn, LocalDate checkOut, int minimumGuests)
            throws SQLException {
        validateDates(checkIn, checkOut);
        if (minimumGuests < 0) throw new ValidationException("Number of guests cannot be negative");
        return dao.availableRooms(checkIn, checkOut, Math.max(1, minimumGuests));
    }

    public List<Reservation> reservations(String search) throws SQLException {
        return dao.findReservations(search == null ? "" : search);
    }

    public Reservation book(Room room, String name, String phone, int guests,
                            LocalDate checkIn, LocalDate checkOut) throws SQLException {
        if (room == null) throw new ValidationException("Select a room first");
        validateDates(checkIn, checkOut);
        if (guests <= 0 || guests > room.getCapacity()) {
            throw new ValidationException("Guests must be between 1 and room capacity (" + room.getCapacity() + ")");
        }
        Guest guest;
        try {
            guest = new Guest(name, phone);
        } catch (IllegalArgumentException error) {
            throw new ValidationException(error.getMessage(), error);
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        long total;
        try {
            total = Math.multiplyExact(nights, room.getNightlyPriceRupees());
        } catch (ArithmeticException error) {
            throw new ValidationException("Stay total is too large", error);
        }
        // DAO rechecks the active overlap inside the same transaction as its INSERT.
        return dao.book(room, guest, guests, checkIn, checkOut, total)
                .orElseThrow(() -> new RoomUnavailableException("This room is no longer available for those dates"));
    }

    public void cancel(long id) throws SQLException {
        if (id <= 0) throw new ValidationException("Select a reservation first");
        Reservation reservation = dao.findById(id)
                .orElseThrow(() -> new ValidationException("Reservation not found"));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationException("Reservation is already cancelled");
        }
        if (!dao.cancel(id)) throw new ValidationException("Reservation has already been cancelled");
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) throw new ValidationException("Select both dates");
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new ValidationException("Check-in cannot be before today");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out must be after check-in");
        }
    }
}
