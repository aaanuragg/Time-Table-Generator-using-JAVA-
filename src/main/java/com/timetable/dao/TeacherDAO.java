package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.DayOfWeek;
import com.timetable.model.Role;
import com.timetable.model.Teacher;
import com.timetable.model.TeacherUnavailability;
import com.timetable.model.TimeSlot;
import com.timetable.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeacherDAO {

    public int createTeacher(String fullName, String username, String plainPassword, double maxWeeklyHours) throws SQLException {
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User user = new User(username, hash, fullName, Role.TEACHER);
        UserDAO userDAO = new UserDAO();
        int userId = userDAO.insert(user);
        if (userId <= 0) {
            return -1;
        }

        Teacher teacher = new Teacher(userId, maxWeeklyHours);
        teacher.setFullName(fullName);
        teacher.setUsername(username);
        return insert(teacher);
    }

    public boolean updateTeacherAndUser(int teacherId, int userId, String fullName, String username, String plainPassword, double maxWeeklyHours) throws SQLException {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.findById(userId);
        if (user != null) {
            user.setFullName(fullName);
            user.setUsername(username);
            userDAO.update(user);

            if (plainPassword != null && !plainPassword.trim().isEmpty()) {
                String hash = BCrypt.hashpw(plainPassword.trim(), BCrypt.gensalt());
                userDAO.updatePassword(userId, hash);
            }
        }
        return updateMaxWeeklyHours(teacherId, maxWeeklyHours);
    }

    public boolean deleteTeacherAndUser(int userId) throws SQLException {
        UserDAO userDAO = new UserDAO();
        return userDAO.delete(userId);
    }

    public int insert(Teacher teacher) throws SQLException {
        String sql = "INSERT INTO teachers (user_id, max_weekly_hours) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, teacher.getUserId());
            ps.setDouble(2, teacher.getMaxWeeklyHours());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    teacher.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public Teacher findById(int id) throws SQLException {
        String sql = "SELECT t.id, t.user_id, t.max_weekly_hours, u.full_name, u.username " +
                     "FROM teachers t " +
                     "JOIN users u ON t.user_id = u.id " +
                     "WHERE t.id = ?";
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

    public Teacher findByUserId(int userId) throws SQLException {
        String sql = "SELECT t.id, t.user_id, t.max_weekly_hours, u.full_name, u.username " +
                     "FROM teachers t " +
                     "JOIN users u ON t.user_id = u.id " +
                     "WHERE t.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Teacher> findAll() throws SQLException {
        List<Teacher> list = new ArrayList<>();
        String sql = "SELECT t.id, t.user_id, t.max_weekly_hours, u.full_name, u.username " +
                     "FROM teachers t " +
                     "JOIN users u ON t.user_id = u.id " +
                     "ORDER BY u.full_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(Teacher teacher) throws SQLException {
        String sql = "UPDATE teachers SET max_weekly_hours = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, teacher.getMaxWeeklyHours());
            ps.setInt(2, teacher.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateMaxWeeklyHours(int teacherId, double maxWeeklyHours) throws SQLException {
        String sql = "UPDATE teachers SET max_weekly_hours = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, maxWeeklyHours);
            ps.setInt(2, teacherId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM teachers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

  

    public boolean addUnavailableSlot(int teacherId, int slotId, String reason) throws SQLException {
        String sql = "INSERT INTO teacher_unavailability (teacher_id, slot_id, reason) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE reason = VALUES(reason)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teacherId);
            ps.setInt(2, slotId);
            ps.setString(3, reason);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeUnavailableSlot(int teacherId, int slotId) throws SQLException {
        String sql = "DELETE FROM teacher_unavailability WHERE teacher_id = ? AND slot_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teacherId);
            ps.setInt(2, slotId);

            return ps.executeUpdate() > 0;
        }
    }

    public List<TeacherUnavailability> getUnavailableSlots(int teacherId) throws SQLException {
        List<TeacherUnavailability> list = new ArrayList<>();
        String sql = "SELECT tu.id, tu.teacher_id, tu.slot_id, tu.reason, " +
                     "       ts.day_of_week, ts.period_number, ts.start_time, ts.end_time " +
                     "FROM teacher_unavailability tu " +
                     "JOIN time_slots ts ON tu.slot_id = ts.id " +
                     "WHERE tu.teacher_id = ? " +
                     "ORDER BY FIELD(ts.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), " +
                     "         ts.period_number";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeacherUnavailability unavail = new TeacherUnavailability(
                            rs.getInt("id"),
                            rs.getInt("teacher_id"),
                            rs.getInt("slot_id"),
                            rs.getString("reason")
                    );
                    TimeSlot slot = new TimeSlot(
                            rs.getInt("slot_id"),
                            DayOfWeek.valueOf(rs.getString("day_of_week")),
                            rs.getInt("period_number"),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime()
                    );
                    unavail.setTimeSlot(slot);
                    list.add(unavail);
                }
            }
        }
        return list;
    }

    private Teacher mapRow(ResultSet rs) throws SQLException {
        return new Teacher(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getDouble("max_weekly_hours"),
                rs.getString("full_name"),
                rs.getString("username")
        );
    }
}