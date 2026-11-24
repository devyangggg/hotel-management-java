package com.example.hotel.service;

import com.example.hotel.dao.Database;
import com.example.hotel.dao.JdbcReservationDao;
import com.example.hotel.model.Reservation;
import com.example.hotel.model.Room;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    @TempDir Path temporaryDirectory;
    Database database;
    ReservationService service;
    Room room;
    LocalDate start;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(temporaryDirectory.resolve("hotel.db"));
        database.initialize();
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
        service = new ReservationService(new JdbcReservationDao(database), clock);
        room = service.rooms().getFirst();
        start = LocalDate.now(clock).plusDays(1);
    }

    private Reservation book(LocalDate in, LocalDate out) throws SQLException {
        return service.book(room, "Ana", "1234567890", 2, in, out);
    }

    @Test
    void overlappingRejectedButAdjacentAndCancelledDatesAvailable() throws Exception {
        Reservation first = book(start, start.plusDays(2));
        assertEquals(2, first.nights());
        assertEquals(4400, first.getTotalRupees());
        assertThrows(RoomUnavailableException.class, () -> book(start.plusDays(1), start.plusDays(3)));
        assertTrue(service.searchAvailable(start.plusDays(1), start.plusDays(2), 2).stream()
                .noneMatch(candidate -> candidate.getId() == room.getId()));
        Reservation adjacent = book(start.plusDays(2), start.plusDays(3));
        service.cancel(first.getId());
        assertEquals(2, service.reservations("").size());
        assertEquals(1, service.searchAvailable(start, start.plusDays(2), 2).stream()
                .filter(candidate -> candidate.getId() == room.getId()).count());
        assertNotEquals(first.getId(), book(start, start.plusDays(2)).getId());
        assertEquals(3, service.reservations("").size());
        assertEquals(adjacent.getId(), service.reservations(Long.toString(adjacent.getId())).getFirst().getId());
    }

    @Test
    void rejectsInvalidDatesAndExcessiveGuests() {
        assertThrows(ValidationException.class, () -> book(start.minusDays(2), start));
        assertThrows(ValidationException.class, () -> book(start, start));
        assertThrows(ValidationException.class,
                () -> service.book(room, "Ana", "1234567890", room.getCapacity() + 1, start, start.plusDays(1)));
        assertThrows(ValidationException.class,
                () -> service.book(room, "Ana", "bad phone", 1, start, start.plusDays(1)));
    }

    @Test
    void failedInsertRollsBackWithoutPartialReservation() throws Exception {
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TRIGGER fail_booking BEFORE INSERT ON reservations "
                    + "BEGIN SELECT RAISE(ABORT, 'simulated disk failure'); END");
        }
        assertThrows(SQLException.class, () -> book(start, start.plusDays(1)));
        assertTrue(service.reservations("").isEmpty());
    }
}
