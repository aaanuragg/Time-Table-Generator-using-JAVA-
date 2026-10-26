package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.ClassSection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassDAO {

    public int insert(ClassSection classSection) throws SQLException {
        String sql = "INSERT INTO classes (name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, classSection.getName());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    classSection.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public ClassSection findById(int id) throws SQLException {
        String sql = "SELECT id, name FROM classes WHERE id = ?";
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

    public ClassSection findByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM classes WHERE name = ?";
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

    public List<ClassSection> findAll() throws SQLException {
        List<ClassSection> list = new ArrayList<>();
        String sql = "SELECT id, name FROM classes ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(ClassSection classSection) throws SQLException {
        String sql = "UPDATE classes SET name = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, classSection.getName());
            ps.setInt(2, classSection.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private ClassSection mapRow(ResultSet rs) throws SQLException {
        return new ClassSection(
                rs.getInt("id"),
                rs.getString("name")
        );
    }
}
