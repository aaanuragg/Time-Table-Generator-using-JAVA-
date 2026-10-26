package com.timetable.ui;

import com.timetable.model.User;
import com.timetable.service.Session;

import javax.swing.*;
import java.awt.*;

public class AdminDashboardFrame extends JFrame {

    private TeacherPanel teacherPanel;
    private SubjectPanel subjectPanel;
    private ClassPanel classPanel;
    private RoomPanel roomPanel;
    private AssignmentPanel assignmentPanel;
    private GeneratePanel generatePanel;
    private ViewTimetablePanel viewTimetablePanel;

    public AdminDashboardFrame() {
        setTitle("Admin Dashboard - Automatic Timetable Generator");
        setSize(1150, 700); // Widen to 1150x700 for full visibility
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Top Header: Welcome Text & Logout Button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        User user = Session.getCurrentUser();
        String fullName = (user != null) ? user.getFullName() : "Admin";

        JLabel welcomeLabel = new JLabel("Welcome, " + fullName + " (Admin)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutButton.addActionListener(e -> logout());

        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        // Center: JTabbedPane with 7 tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        teacherPanel = new TeacherPanel();
        subjectPanel = new SubjectPanel();
        classPanel = new ClassPanel();
        roomPanel = new RoomPanel();
        assignmentPanel = new AssignmentPanel();
        generatePanel = new GeneratePanel();
        viewTimetablePanel = new ViewTimetablePanel();

        tabbedPane.addTab("Teachers", teacherPanel);
        tabbedPane.addTab("Subjects", subjectPanel);
        tabbedPane.addTab("Classes", classPanel);
        tabbedPane.addTab("Rooms", roomPanel);
        tabbedPane.addTab("Assignments", assignmentPanel);
        tabbedPane.addTab("Generate", generatePanel);
        tabbedPane.addTab("View Timetable", viewTimetablePanel);

        // Refresh tab data whenever the user switches tabs
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            if (selected == teacherPanel) {
                teacherPanel.loadData();
            } else if (selected == subjectPanel) {
                subjectPanel.loadData();
            } else if (selected == classPanel) {
                classPanel.loadData();
            } else if (selected == roomPanel) {
                roomPanel.loadData();
            } else if (selected == assignmentPanel) {
                assignmentPanel.loadData();
            } else if (selected == viewTimetablePanel) {
                viewTimetablePanel.loadData();
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void logout() {
        Session.clear();
        dispose();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
    }
}
