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
}
