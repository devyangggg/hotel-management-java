package com.example.hotel.util;

import com.example.hotel.dao.Database;
import com.example.hotel.dao.JdbcReservationDao;
import com.example.hotel.model.Guest;
import com.example.hotel.model.Reservation;
import com.example.hotel.model.ReservationStatus;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ReservationFilesTest {
    @TempDir Path temporaryDirectory;

    @Test
    void exportAndSnapshotRoundTripAreReadOnly() throws Exception {
        Database database = new Database(temporaryDirectory.resolve("hotel.db"));
        database.initialize();
        JdbcReservationDao dao = new JdbcReservationDao(database);
        LocalDate in = LocalDate.now().plusDays(1);
        Reservation reservation = dao.book(dao.listRooms().getFirst(), new Guest("Ana", "1234567"),
                2, in, in.plusDays(2), 4400).orElseThrow();
        Path text = temporaryDirectory.resolve("stay.txt");
        ReservationFiles.exportText(reservation, text);
        String exported = Files.readString(text, StandardCharsets.UTF_8);
        assertTrue(exported.contains("Reservation #" + reservation.getId()));
        assertTrue(exported.contains("Total: ₹4400"));
        Path snapshot = temporaryDirectory.resolve("stay.ser");
        ReservationFiles.saveSnapshot(reservation, snapshot);
        assertEquals(exported, ReservationFiles.readSnapshot(snapshot).display());
        assertEquals(ReservationStatus.ACTIVE, dao.findById(reservation.getId()).orElseThrow().getStatus());
        assertEquals(1, dao.findReservations("").size());
    }

    @Test
    void refusesUnknownObjectsAndCorruptBytes() throws Exception {
        Path unexpected = temporaryDirectory.resolve("unexpected.ser");
        try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(unexpected))) {
            stream.writeObject(new ArrayList<String>());
        }
        assertThrows(IOException.class, () -> ReservationFiles.readSnapshot(unexpected));
        Path corrupt = temporaryDirectory.resolve("corrupt.ser");
        Files.writeString(corrupt, "not serialized data");
        assertThrows(IOException.class, () -> ReservationFiles.readSnapshot(corrupt));
    }
}
