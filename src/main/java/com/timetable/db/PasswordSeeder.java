package com.timetable.db;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PasswordSeeder {

    public static void seedPasswords() {
        System.out.println("Seeding BCrypt password hashes into database...");

        String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt(12));
        String teacherHash = BCrypt.hashpw("teacher123", BCrypt.gensalt(12));

        String updateAdminSql = "UPDATE users SET password_hash = ? WHERE role = 'ADMIN'";
        String updateTeacherSql = "UPDATE users SET password_hash = ? WHERE role = 'TEACHER'";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement psAdmin = conn.prepareStatement(updateAdminSql)) {
                psAdmin.setString(1, adminHash);
                int adminsUpdated = psAdmin.executeUpdate();
                System.out.println("Updated " + adminsUpdated + " ADMIN user(s) password to 'admin123'");
            }

            try (PreparedStatement psTeacher = conn.prepareStatement(updateTeacherSql)) {
                psTeacher.setString(1, teacherHash);
                int teachersUpdated = psTeacher.executeUpdate();
                System.out.println("Updated " + teachersUpdated + " TEACHER user(s) password to 'teacher123'");
            }

            System.out.println("Password seeding completed successfully.");
        } catch (SQLException e) {
            System.err.println("Failed to seed passwords: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        seedPasswords();
    }
}
