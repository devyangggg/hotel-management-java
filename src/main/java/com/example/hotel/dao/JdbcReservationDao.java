package com.example.hotel.dao;

import com.example.hotel.model.DeluxeRoom;
import com.example.hotel.model.Guest;
import com.example.hotel.model.Reservation;
import com.example.hotel.model.ReservationStatus;
import com.example.hotel.model.Room;
import com.example.hotel.model.StandardRoom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReservationDao implements ReservationDao {
    private static final String ROOM_COLUMNS = "r.id AS room_id, r.number, r.type, r.capacity, r.nightly_price_rupees";
    private static final String RESERVATION_COLUMNS = "b.id AS reservation_id, b.guest_name, b.guest_phone, "
            + "b.guests, b.check_in, b.check_out, b.total_rupees, b.status, " + ROOM_COLUMNS;
    private final Database database;

    public JdbcReservationDao(Database database) {
        this.database = database;
    }

    @Override
    public List<Room> listRooms() throws SQLException {
        String sql = "SELECT " + ROOM_COLUMNS + " FROM rooms r ORDER BY r.nightly_price_rupees, r.number";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rows.next()) rooms.add(mapRoom(rows));
            return rooms;
        }
    }

    @Override
    public List<Room> availableRooms(LocalDate checkIn, LocalDate checkOut, int minimumCapacity)
            throws SQLException {
        String sql = "SELECT " + ROOM_COLUMNS + " FROM rooms r WHERE r.capacity >= ? "
                + "AND NOT EXISTS (SELECT 1 FROM reservations b WHERE b.room_id = r.id "
                + "AND b.status = 'ACTIVE' AND b.check_in < ? AND b.check_out > ?) "
                + "ORDER BY r.nightly_price_rupees, r.number";
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, minimumCapacity);
            statement.setString(2, checkOut.toString());
            statement.setString(3, checkIn.toString());
            try (ResultSet rows = statement.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (rows.next()) rooms.add(mapRoom(rows));
                return rooms;
            }
        }
    }

    @Override
    public List<Reservation> findReservations(String search) throws SQLException {
        String sql = "SELECT " + RESERVATION_COLUMNS + " FROM reservations b "
                + "JOIN rooms r ON r.id = b.room_id WHERE CAST(b.id AS TEXT) LIKE ? "
                + "OR LOWER(b.guest_name) LIKE LOWER(?) ORDER BY b.id DESC";
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            String pattern = "%" + search.trim() + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet rows = statement.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rows.next()) reservations.add(mapReservation(rows));
                return reservations;
            }
        }
    }

    @Override
    public Optional<Reservation> findById(long id) throws SQLException {
        String sql = "SELECT " + RESERVATION_COLUMNS + " FROM reservations b "
                + "JOIN rooms r ON r.id = b.room_id WHERE b.id = ?";
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapReservation(rows)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Reservation> book(Room room, Guest guest, int guests, LocalDate checkIn,
                                      LocalDate checkOut, long totalRupees) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                String overlap = "SELECT 1 FROM reservations WHERE room_id = ? AND status = 'ACTIVE' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1";
                try (PreparedStatement statement = connection.prepareStatement(overlap)) {
                    statement.setLong(1, room.getId());
                    statement.setString(2, checkOut.toString());
                    statement.setString(3, checkIn.toString());
                    try (ResultSet rows = statement.executeQuery()) {
                        if (rows.next()) {
                            connection.rollback();
                            return Optional.empty();
                        }
                    }
                }
                String insert = "INSERT INTO reservations(room_id, guest_name, guest_phone, guests, "
                        + "check_in, check_out, total_rupees, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                    statement.setLong(1, room.getId());
                    statement.setString(2, guest.getName());
                    statement.setString(3, guest.getPhone());
                    statement.setInt(4, guests);
                    statement.setString(5, checkIn.toString());
                    statement.setString(6, checkOut.toString());
                    statement.setLong(7, totalRupees);
                    statement.executeUpdate();
                }
                long id;
                try (Statement statement = connection.createStatement();
                     ResultSet row = statement.executeQuery("SELECT last_insert_rowid()")) {
                    row.next();
                    id = row.getLong(1);
                }
                connection.commit();
                return Optional.of(new Reservation(id, guest, room, guests, checkIn, checkOut,
                        totalRupees, ReservationStatus.ACTIVE));
            } catch (SQLException | RuntimeException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    @Override
    public boolean cancel(long id) throws SQLException {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE reservations SET status = 'CANCELLED' WHERE id = ? AND status = 'ACTIVE'")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static Room mapRoom(ResultSet rows) throws SQLException {
        long id = rows.getLong("room_id");
        String number = rows.getString("number");
        int capacity = rows.getInt("capacity");
        int price = rows.getInt("nightly_price_rupees");
        return switch (rows.getString("type")) {
            case "STANDARD" -> new StandardRoom(id, number, capacity, price);
            case "DELUXE" -> new DeluxeRoom(id, number, capacity, price);
            default -> throw new SQLException("Unknown room type in database");
        };
    }

    private static Reservation mapReservation(ResultSet rows) throws SQLException {
        return new Reservation(rows.getLong("reservation_id"),
                new Guest(rows.getString("guest_name"), rows.getString("guest_phone")),
                mapRoom(rows), rows.getInt("guests"), LocalDate.parse(rows.getString("check_in")),
                LocalDate.parse(rows.getString("check_out")), rows.getLong("total_rupees"),
                ReservationStatus.valueOf(rows.getString("status")));
    }
}
