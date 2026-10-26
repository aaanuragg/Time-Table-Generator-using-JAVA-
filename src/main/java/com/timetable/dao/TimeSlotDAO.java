package com.timetable.dao;

import com.timetable.db.DBConnection;
import com.timetable.model.DayOfWeek;
import com.timetable.model.TimeSlot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TimeSlotDAO {

    public int insert(TimeSlot slot) throws SQLException {
        String sql = "INSERT INTO time_slots (day_of_week, period_number, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, slot.getDayOfWeek().name());
            ps.setInt(2, slot.getPeriodNumber());
            ps.setTime(3, Time.valueOf(slot.getStartTime()));
            ps.setTime(4, Time.valueOf(slot.getEndTime()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    slot.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public TimeSlot findById(int id) throws SQLException {
        String sql = "SELECT id, day_of_week, period_number, start_time, end_time FROM time_slots WHERE id = ?";
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

    public TimeSlot findByDayAndPeriod(DayOfWeek dayOfWeek, int periodNumber) throws SQLException {
        String sql = "SELECT id, day_of_week, period_number, start_time, end_time " +
                     "FROM time_slots WHERE day_of_week = ? AND period_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dayOfWeek.name());
            ps.setInt(2, periodNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<TimeSlot> findAll() throws SQLException {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT id, day_of_week, period_number, start_time, end_time " +
                     "FROM time_slots " +
                     "ORDER BY FIELD(day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'), period_number";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(TimeSlot slot) throws SQLException {
        String sql = "UPDATE time_slots SET day_of_week = ?, period_number = ?, start_time = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, slot.getDayOfWeek().name());
            ps.setInt(2, slot.getPeriodNumber());
            ps.setTime(3, Time.valueOf(slot.getStartTime()));
            ps.setTime(4, Time.valueOf(slot.getEndTime()));
            ps.setInt(5, slot.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM time_slots WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private TimeSlot mapRow(ResultSet rs) throws SQLException {
        return new TimeSlot(
                rs.getInt("id"),
                DayOfWeek.valueOf(rs.getString("day_of_week")),
                rs.getInt("period_number"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime()
        );
    }
}
