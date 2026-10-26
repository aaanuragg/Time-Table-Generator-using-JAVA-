package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.DayOfWeek;
import com.timetable.model.TimetableEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TimetableDAO {

    public int insert(TimetableEntry entry) throws SQLException {
        String sql = "INSERT INTO timetable_entries (assignment_id, class_id, teacher_id, subject_id, room_id, slot_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (entry.getAssignmentId() != null) {
                ps.setInt(1, entry.getAssignmentId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, entry.getClassId());
            ps.setInt(3, entry.getTeacherId());
            ps.setInt(4, entry.getSubjectId());
            ps.setInt(5, entry.getRoomId());
            ps.setInt(6, entry.getSlotId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    entry.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public List<TimetableEntry> getTimetableByTeacher(int teacherId) throws SQLException {
        String sql = "SELECT te.id, te.assignment_id, te.class_id, te.teacher_id, te.subject_id, te.room_id, te.slot_id, " +
                     "       c.name AS class_name, u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, r.name AS room_name, r.room_type AS room_type, " +
                     "       ts.day_of_week, ts.period_number, ts.start_time, ts.end_time " +
                     "FROM timetable_entries te " +
                     "JOIN classes c ON te.class_id = c.id " +
                     "JOIN teachers t ON te.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON te.subject_id = s.id " +
                     "JOIN rooms r ON te.room_id = r.id " +
                     "JOIN time_slots ts ON te.slot_id = ts.id " +
                     "WHERE te.teacher_id = ? " +
                     "ORDER BY FIELD(ts.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), " +
                     "         ts.period_number";

        return queryList(sql, teacherId);
    }

    public List<TimetableEntry> getTimetableByClass(int classId) throws SQLException {
        String sql = "SELECT te.id, te.assignment_id, te.class_id, te.teacher_id, te.subject_id, te.room_id, te.slot_id, " +
                     "       c.name AS class_name, u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, r.name AS room_name, r.room_type AS room_type, " +
                     "       ts.day_of_week, ts.period_number, ts.start_time, ts.end_time " +
                     "FROM timetable_entries te " +
                     "JOIN classes c ON te.class_id = c.id " +
                     "JOIN teachers t ON te.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON te.subject_id = s.id " +
                     "JOIN rooms r ON te.room_id = r.id " +
                     "JOIN time_slots ts ON te.slot_id = ts.id " +
                     "WHERE te.class_id = ? " +
                     "ORDER BY FIELD(ts.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), " +
                     "         ts.period_number";

        return queryList(sql, classId);
    }

    public List<TimetableEntry> getTimetableByRoom(int roomId) throws SQLException {
        String sql = "SELECT te.id, te.assignment_id, te.class_id, te.teacher_id, te.subject_id, te.room_id, te.slot_id, " +
                     "       c.name AS class_name, u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, r.name AS room_name, r.room_type AS room_type, " +
                     "       ts.day_of_week, ts.period_number, ts.start_time, ts.end_time " +
                     "FROM timetable_entries te " +
                     "JOIN classes c ON te.class_id = c.id " +
                     "JOIN teachers t ON te.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON te.subject_id = s.id " +
                     "JOIN rooms r ON te.room_id = r.id " +
                     "JOIN time_slots ts ON te.slot_id = ts.id " +
                     "WHERE te.room_id = ? " +
                     "ORDER BY FIELD(ts.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), " +
                     "         ts.period_number";

        return queryList(sql, roomId);
    }

    public List<TimetableEntry> findAll() throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = "SELECT te.id, te.assignment_id, te.class_id, te.teacher_id, te.subject_id, te.room_id, te.slot_id, " +
                     "       c.name AS class_name, u.full_name AS teacher_name, s.name AS subject_name, s.code AS subject_code, " +
                     "       s.requires_lab AS requires_lab, r.name AS room_name, r.room_type AS room_type, " +
                     "       ts.day_of_week, ts.period_number, ts.start_time, ts.end_time " +
                     "FROM timetable_entries te " +
                     "JOIN classes c ON te.class_id = c.id " +
                     "JOIN teachers t ON te.teacher_id = t.id " +
                     "JOIN users u ON t.user_id = u.id " +
                     "JOIN subjects s ON te.subject_id = s.id " +
                     "JOIN rooms r ON te.room_id = r.id " +
                     "JOIN time_slots ts ON te.slot_id = ts.id " +
                     "ORDER BY c.name ASC, FIELD(ts.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), ts.period_number";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM timetable_entries";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean deleteAll() throws SQLException {
        String sql = "DELETE FROM timetable_entries";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.executeUpdate();
            return true;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM timetable_entries WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private List<TimetableEntry> queryList(String sql, int paramId) throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paramId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private TimetableEntry mapRow(ResultSet rs) throws SQLException {
        int assignmentIdVal = rs.getInt("assignment_id");
        Integer assignmentId = rs.wasNull() ? null : assignmentIdVal;

        TimetableEntry entry = new TimetableEntry(
                rs.getInt("id"),
                assignmentId,
                rs.getInt("class_id"),
                rs.getInt("teacher_id"),
                rs.getInt("subject_id"),
                rs.getInt("room_id"),
                rs.getInt("slot_id")
        );

        entry.setClassName(rs.getString("class_name"));
        entry.setTeacherName(rs.getString("teacher_name"));
        entry.setSubjectName(rs.getString("subject_name"));
        entry.setRoomName(rs.getString("room_name"));
        entry.setDayOfWeek(DayOfWeek.valueOf(rs.getString("day_of_week")));
        entry.setPeriodNumber(rs.getInt("period_number"));
        entry.setStartTime(rs.getTime("start_time").toLocalTime());
        entry.setEndTime(rs.getTime("end_time").toLocalTime());

        try {
            entry.setSubjectCode(rs.getString("subject_code"));
        } catch (SQLException ignored) {
        }
        try {
            entry.setRequiresLab(rs.getBoolean("requires_lab"));
        } catch (SQLException ignored) {
        }
        try {
            entry.setRoomType(rs.getString("room_type"));
        } catch (SQLException ignored) {
        }

        return entry;
    }
}
