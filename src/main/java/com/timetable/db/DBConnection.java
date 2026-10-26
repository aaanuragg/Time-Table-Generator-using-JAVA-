package com.timetable.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;
    private static String loadError = null;

    private static synchronized void loadConfig() {
        if (loaded) return;
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                loadError = "Unable to find 'db.properties' in classpath (expected in src/main/resources/db.properties).";
                return;
            }
            properties.load(input);
            Class.forName("com.mysql.cj.jdbc.Driver");
            loaded = true;
        } catch (Exception e) {
            loadError = e.getMessage();
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (!loaded) {
            loadConfig();
        }
        if (loadError != null) {
            throw new SQLException("Database configuration error: " + loadError);
        }

        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        return DriverManager.getConnection(url, user, password);
    }
}
