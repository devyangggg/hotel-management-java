package com.example.hotel.dao;

import com.example.hotel.model.Guest;
import com.example.hotel.model.Reservation;
import com.example.hotel.model.Room;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationDao {
    List<Room> listRooms() throws SQLException;
    List<Room> availableRooms(LocalDate checkIn, LocalDate checkOut, int minimumCapacity) throws SQLException;
    List<Reservation> findReservations(String search) throws SQLException;
    Optional<Reservation> findById(long id) throws SQLException;
    // Empty means an active stay already occupies the room.
    Optional<Reservation> book(Room room, Guest guest, int guests, LocalDate checkIn,
                               LocalDate checkOut, long totalRupees) throws SQLException;
    boolean cancel(long id) throws SQLException;
}
