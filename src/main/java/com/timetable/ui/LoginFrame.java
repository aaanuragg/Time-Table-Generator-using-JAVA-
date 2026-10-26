package com.timetable.ui;

import com.timetable.dao.UserDAO;
import com.timetable.model.Role;
import com.timetable.model.User;
import com.timetable.service.Session;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel errorLabel;

    public LoginFrame() {
        setTitle("Timetable Generator - Login");
        setSize(400, 330);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel titleLabel = new JLabel("Login");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Pressing Enter in password field or clicking button triggers login
        passwordField.addActionListener(e -> handleLogin());
        usernameField.addActionListener(e -> handleLogin());
        loginButton.addActionListener(e -> handleLogin());

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(passLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(errorLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(loginButton);

        add(panel);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty()) {
            errorLabel.setText("Please enter your username.");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            errorLabel.setText("Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        errorLabel.setText(" ");
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Perform DB authentication on a background thread so UI never freezes
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                UserDAO userDAO = new UserDAO();
                return userDAO.authenticate(username, password);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                loginButton.setText("Login");

                try {
                    User user = get();
                    if (user != null) {
                        Session.setCurrentUser(user);
                        dispose();

                        if (user.getRole() == Role.ADMIN) {
                            new AdminDashboardFrame().setVisible(true);
                        } else {
                            new TeacherDashboardFrame().setVisible(true);
                        }
                    } else {
                        errorLabel.setText("Invalid username or password.");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception e) {
                    errorLabel.setText("Database error: " + e.getMessage());
                    passwordField.setText("");
                }
            }
        };

        worker.execute();
    }
}
