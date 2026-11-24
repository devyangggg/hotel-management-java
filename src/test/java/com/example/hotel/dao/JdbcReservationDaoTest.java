package com.example.hotel.dao;

import com.example.hotel.model.DeluxeRoom;
import com.example.hotel.model.Guest;
import com.example.hotel.model.ReservationStatus;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JdbcReservationDaoTest {
    @TempDir Path temporaryDirectory;

    @Test
    void initializationIsRepeatSafeAndMappingsSurviveReopen() throws Exception {
        Database database = new Database(temporaryDirectory.resolve("hotel.db"));
        database.initialize();
        database.initialize();
        JdbcReservationDao dao = new JdbcReservationDao(database);
        assertEquals(4, dao.listRooms().size());
        assertInstanceOf(DeluxeRoom.class, dao.listRooms().get(2));
        LocalDate today = LocalDate.now().plusDays(2);
        var booked = dao.book(dao.listRooms().getFirst(), new Guest("Ana", "1234567"),
                1, today, today.plusDays(1), 2200).orElseThrow();
        assertEquals(booked.getId(), new JdbcReservationDao(database).findById(booked.getId()).orElseThrow().getId());
        assertEquals(1, dao.findReservations("ana").size());
        assertTrue(dao.cancel(booked.getId()));
        assertEquals(ReservationStatus.CANCELLED, dao.findById(booked.getId()).orElseThrow().getStatus());
        assertFalse(dao.cancel(booked.getId()));
    }

    @Test
    void foreignKeysAreEnabledOnEveryConnection() throws Exception {
        Database database = new Database(temporaryDirectory.resolve("foreign.db"));
        database.initialize();
        try (var connection = database.open(); var statement = connection.prepareStatement(
                "INSERT INTO reservations(room_id, guest_name, guest_phone, guests, check_in, check_out, total_rupees, status) "
                        + "VALUES (9999, 'Ana', '1234567', 1, '2030-01-01', '2030-01-02', 2200, 'ACTIVE')")) {
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }
}
