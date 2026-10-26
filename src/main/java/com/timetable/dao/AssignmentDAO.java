package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.Assignment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO {

    public int insert(Assignment assignment) throws SQLException {
        String sql = "INSERT INTO assignments (teacher_id, subject_id, class_id, periods_per_week) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, assignment.getTeacherId());
            ps.setInt(2, assignment.getSubjectId());
            ps.setInt(3, assignment.getClassId());
            ps.setInt(4, assignment.getPeriodsPerWeek());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    assignment.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public Assignment findById(int id) throws SQLException {
        String sql = "SELECT a.id, a.teacher_id, a.subject_id, a.class_id, a.periods_per_week, " +
                     "       u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, c.name AS class_name " +
                     "FROM assignments a " +
                     "JOIN teachers t ON a.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON a.subject_id = s.id " +
                     "JOIN classes c ON a.class_id = c.id " +
                     "WHERE a.id = ?";
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

    public List<Assignment> findAll() throws SQLException {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.id, a.teacher_id, a.subject_id, a.class_id, a.periods_per_week, " +
                     "       u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, c.name AS class_name " +
                     "FROM assignments a " +
                     "JOIN teachers t ON a.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON a.subject_id = s.id " +
                     "JOIN classes c ON a.class_id = c.id " +
                     "ORDER BY c.name ASC, s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Assignment> findByTeacherId(int teacherId) throws SQLException {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.id, a.teacher_id, a.subject_id, a.class_id, a.periods_per_week, " +
                     "       u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, c.name AS class_name " +
                     "FROM assignments a " +
                     "JOIN teachers t ON a.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON a.subject_id = s.id " +
                     "JOIN classes c ON a.class_id = c.id " +
                     "WHERE a.teacher_id = ? " +
                     "ORDER BY c.name ASC, s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Assignment> findByClassId(int classId) throws SQLException {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.id, a.teacher_id, a.subject_id, a.class_id, a.periods_per_week, " +
                     "       u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, c.name AS class_name " +
                     "FROM assignments a " +
                     "JOIN teachers t ON a.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON a.subject_id = s.id " +
                     "JOIN classes c ON a.class_id = c.id " +
                     "WHERE a.class_id = ? " +
                     "ORDER BY s.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean update(Assignment assignment) throws SQLException {
        String sql = "UPDATE assignments SET teacher_id = ?, subject_id = ?, class_id = ?, periods_per_week = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assignment.getTeacherId());
            ps.setInt(2, assignment.getSubjectId());
            ps.setInt(3, assignment.getClassId());
            ps.setInt(4, assignment.getPeriodsPerWeek());
            ps.setInt(5, assignment.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM assignments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Assignment mapRow(ResultSet rs) throws SQLException {
        Assignment assignment = new Assignment(
                rs.getInt("id"),
                rs.getInt("teacher_id"),
                rs.getInt("subject_id"),
                rs.getInt("class_id"),
                rs.getInt("periods_per_week")
        );
        assignment.setTeacherName(rs.getString("teacher_name"));
        assignment.setSubjectName(rs.getString("subject_name"));
        assignment.setClassName(rs.getString("class_name"));

        try {
            assignment.setSubjectCode(rs.getString("subject_code"));
        } catch (SQLException ignored) {
        }

        try {
            assignment.setRequiresLab(rs.getBoolean("requires_lab"));
        } catch (SQLException ignored) {
        }

        return assignment;
    }
}
