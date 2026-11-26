package com.example.hotel.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    @Test
    void roomPolymorphismAndPriceOrdering() {
        List<Room> rooms = new ArrayList<>(List.of(
                new DeluxeRoom(2, "202", 3, 4500), new StandardRoom(1, "101", 2, 2200)));
        rooms.sort(Comparator.comparingInt(Room::getNightlyPriceRupees));
        assertEquals("101", rooms.getFirst().getNumber());
        assertEquals("Standard — Wi-Fi", rooms.getFirst().description());
        assertEquals("Deluxe — Wi-Fi and breakfast", rooms.get(1).description());
    }

    @Test
    void guestDetailsAndNightCount() {
        assertThrows(IllegalArgumentException.class, () -> new Guest(" ", "1234567"));
        assertThrows(IllegalArgumentException.class, () -> new Guest("Ana", "123-4567"));
        Reservation stay = new Reservation(1, new Guest("Ana", "+1234567890"),
                new StandardRoom(1, "101", 2, 2200), 2,
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 2), 4400,
                ReservationStatus.ACTIVE);
        assertEquals(2, stay.nights());
    }

    @Test
    void reservationRejectsInvalidValuesWithOriginalMessage() {
        Guest guest = new Guest("Ana", "+1234567890");
        Room room = new StandardRoom(1, "101", 2, 2200);
        LocalDate checkIn = LocalDate.of(2026, 1, 31);
        LocalDate checkOut = LocalDate.of(2026, 2, 2);

        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(0, guest, room, 2, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(-1, guest, room, 2, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, guest, room, 0, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, guest, room, -1, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, guest, room, 3, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, guest, room, 2, checkIn, checkOut, -1,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, guest, room, 2, checkIn, checkIn, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, guest, room, 2, checkIn, checkIn.minusDays(1), 4400,
                        ReservationStatus.ACTIVE)).getMessage());
    }

    @Test
    void reservationRejectsMissingArgumentsWithOriginalNullMessages() {
        Guest guest = new Guest("Ana", "+1234567890");
        Room room = new StandardRoom(1, "101", 2, 2200);
        LocalDate checkIn = LocalDate.of(2026, 1, 31);
        LocalDate checkOut = LocalDate.of(2026, 2, 2);

        assertEquals("room", assertThrows(NullPointerException.class,
                () -> new Reservation(1, guest, null, 2, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("checkIn", assertThrows(NullPointerException.class,
                () -> new Reservation(1, guest, room, 2, null, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("checkOut", assertThrows(NullPointerException.class,
                () -> new Reservation(1, guest, room, 2, checkIn, null, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("guest", assertThrows(NullPointerException.class,
                () -> new Reservation(1, null, room, 2, checkIn, checkOut, 4400,
                        ReservationStatus.ACTIVE)).getMessage());
        assertEquals("status", assertThrows(NullPointerException.class,
                () -> new Reservation(1, guest, room, 2, checkIn, checkOut, 4400,
                        null)).getMessage());
    }

    @Test
    void reservationPreservesValidationOrderForCombinedInvalidArguments() {
        Room room = new StandardRoom(1, "101", 2, 2200);
        LocalDate checkIn = LocalDate.of(2026, 1, 31);
        LocalDate checkOut = LocalDate.of(2026, 2, 2);

        // Invalid IDs and guest counts short-circuit before the room null check.
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(0, null, null, 0, null, null, -1, null)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, null, null, 0, null, null, -1, null)).getMessage());

        // Room validation precedes total and date validation.
        assertEquals("room", assertThrows(NullPointerException.class,
                () -> new Reservation(1, null, null, 2, null, null, -1, null)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, null, room, 3, null, null, -1, null)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, null, room, 2, null, null, -1, null)).getMessage());

        // Checkout is checked before check-in, and both precede guest and status.
        assertEquals("checkOut", assertThrows(NullPointerException.class,
                () -> new Reservation(1, null, room, 2, null, null, 4400, null)).getMessage());
        assertEquals("checkIn", assertThrows(NullPointerException.class,
                () -> new Reservation(1, null, room, 2, null, checkOut, 4400, null)).getMessage());
        assertEquals("Invalid reservation details", assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, null, room, 2, checkIn, checkIn, 4400, null)).getMessage());
        assertEquals("guest", assertThrows(NullPointerException.class,
                () -> new Reservation(1, null, room, 2, checkIn, checkOut, 4400, null)).getMessage());
    }
}
