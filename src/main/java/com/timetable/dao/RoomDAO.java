package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    public int insert(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (name, capacity, room_type) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, room.getName());
            ps.setInt(2, room.getCapacity());
            ps.setString(3, room.getRoomType());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    room.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public Room findById(int id) throws SQLException {
        String sql = "SELECT id, name, capacity, room_type FROM rooms WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Room findByName(String name) throws SQLException {
        String sql = "SELECT id, name, capacity, room_type FROM rooms WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Room> findAll() throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, name, capacity, room_type FROM rooms ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(Room room) throws SQLException {
        String sql = "UPDATE rooms SET name = ?, capacity = ?, room_type = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, room.getName());
            ps.setInt(2, room.getCapacity());
            ps.setString(3, room.getRoomType());
            ps.setInt(4, room.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        String roomType = "CLASSROOM";
        try {
            String val = rs.getString("room_type");
            if (val != null) {
                roomType = val;
            }
        } catch (SQLException ignored) {
        }

        return new Room(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("capacity"),
                roomType
        );
    }
}
