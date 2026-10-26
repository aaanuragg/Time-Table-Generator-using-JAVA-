package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    public int insert(Subject subject) throws SQLException {
        String sql = "INSERT INTO subjects (code, name, requires_lab) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setBoolean(3, subject.isRequiresLab());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    subject.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public Subject findById(int id) throws SQLException {
        String sql = "SELECT id, code, name, requires_lab FROM subjects WHERE id = ?";
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

    public Subject findByCode(String code) throws SQLException {
        String sql = "SELECT id, code, name, requires_lab FROM subjects WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Subject> findAll() throws SQLException {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT id, code, name, requires_lab FROM subjects ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(Subject subject) throws SQLException {
        String sql = "UPDATE subjects SET code = ?, name = ?, requires_lab = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, subject.getCode());
            ps.setString(2, subject.getName());
            ps.setBoolean(3, subject.isRequiresLab());
            ps.setInt(4, subject.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        boolean requiresLab = false;
        try {
            requiresLab = rs.getBoolean("requires_lab");
        } catch (SQLException ignored) {
        }

        return new Subject(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("name"),
                requiresLab
        );
    }
}
