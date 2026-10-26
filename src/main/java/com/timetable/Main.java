package com.timetable;

import com.formdev.flatlaf.FlatLightLaf;
import com.timetable.db.DBConnection;
import com.timetable.ui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        FlatLightLaf.setup();

        // Test database connectivity before launching the UI
        try (Connection conn = DBConnection.getConnection()) {
            // Database is available
        } catch (Exception e) {
            String message = "Unable to connect to the MySQL database.\n\n" +
                    "Please check the following:\n" +
                    "1. MySQL server is installed and currently running.\n" +
                    "2. 'src/main/resources/db.properties' exists with your database details:\n" +
                    "   - db.url (e.g., jdbc:mysql://localhost:3306/timetable_db)\n" +
                    "   - db.user (e.g., root)\n" +
                    "   - db.password (your MySQL password)\n" +
                    "3. The timetable database exists (run database/setup_all.sql to set it up).\n\n" +
                    "Error details:\n" + e.getMessage();

            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Database Connection Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
