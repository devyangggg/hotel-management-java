package com.example.hotel.dao;

import org.sqlite.SQLiteConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class Database {
    private final Path path;

    public Database(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public Connection open() throws SQLException {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new SQLException("Cannot create database directory", exception);
        }
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        // SQLite acquires the write lock before the availability SELECT in a booking transaction.
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath(), config.toProperties());
    }

    public void initialize() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        id INTEGER PRIMARY KEY,
                        number TEXT NOT NULL UNIQUE,
                        type TEXT NOT NULL CHECK (type IN ('STANDARD', 'DELUXE')),
                        capacity INTEGER NOT NULL CHECK (capacity > 0),
                        nightly_price_rupees INTEGER NOT NULL CHECK (nightly_price_rupees >= 0)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INTEGER PRIMARY KEY,
                        room_id INTEGER NOT NULL REFERENCES rooms(id),
                        guest_name TEXT NOT NULL,
                        guest_phone TEXT NOT NULL,
                        guests INTEGER NOT NULL CHECK (guests > 0),
                        check_in TEXT NOT NULL,
                        check_out TEXT NOT NULL,
                        total_rupees INTEGER NOT NULL CHECK (total_rupees >= 0),
                        status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
                        CHECK (check_out > check_in)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_reservations_room_status_dates
                    ON reservations(room_id, status, check_in, check_out)
                    """);
            String insert = """
                    INSERT INTO rooms(number, type, capacity, nightly_price_rupees)
                    VALUES (?, ?, ?, ?) ON CONFLICT(number) DO NOTHING
                    """;
            try (PreparedStatement seed = connection.prepareStatement(insert)) {
                addRoom(seed, "101", "STANDARD", 2, 2200);
                addRoom(seed, "102", "STANDARD", 3, 2700);
                addRoom(seed, "201", "DELUXE", 2, 4200);
                addRoom(seed, "202", "DELUXE", 4, 5500);
            }
        }
    }

    private static void addRoom(PreparedStatement statement, String number, String type,
                                int capacity, int price) throws SQLException {
        statement.setString(1, number);
        statement.setString(2, type);
        statement.setInt(3, capacity);
        statement.setInt(4, price);
        statement.executeUpdate();
    }
}
