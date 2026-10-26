package com.timetable.model;

public class Teacher {
    private int id;
    private int userId;
    private double maxWeeklyHours;

    // Optional convenience fields populated from joined 'users' table
    private String fullName;
    private String username;

    public Teacher() {
    }

    public Teacher(int userId, double maxWeeklyHours) {
        this.userId = userId;
        this.maxWeeklyHours = maxWeeklyHours;
    }

    public Teacher(int id, int userId, double maxWeeklyHours) {
        this.id = id;
        this.userId = userId;
        this.maxWeeklyHours = maxWeeklyHours;
    }

    public Teacher(int id, int userId, double maxWeeklyHours, String fullName, String username) {
        this.id = id;
        this.userId = userId;
        this.maxWeeklyHours = maxWeeklyHours;
        this.fullName = fullName;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getMaxWeeklyHours() {
        return maxWeeklyHours;
    }

    public void setMaxWeeklyHours(double maxWeeklyHours) {
        this.maxWeeklyHours = maxWeeklyHours;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return fullName != null ? fullName : "Teacher #" + id;
    }
}
