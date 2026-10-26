package com.timetable.ui;

import com.timetable.dao.AssignmentDAO;
import com.timetable.dao.TeacherDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.dao.UserDAO;
import com.timetable.model.Teacher;
import com.timetable.model.User;
import com.timetable.service.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TeacherPanel extends JPanel {

    private final TeacherDAO teacherDAO = new TeacherDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final TimetableDAO timetableDAO = new TimetableDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Teacher> teacherList = new ArrayList<>();

    public TeacherPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addButton = new JButton("Add Teacher");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteTeacher());
        refreshButton.addActionListener(e -> loadData());

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);

        String[] columns = {"ID", "Full Name", "Username", "Max Hours / Week"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void loadData() {
        SwingWorker<List<Teacher>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Teacher> doInBackground() throws Exception {
                return teacherDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    teacherList = get();
                    tableModel.setRowCount(0);
                    for (Teacher t : teacherList) {
                        tableModel.addRow(new Object[]{
                                t.getId(),
                                t.getFullName(),
                                t.getUsername(),
                                t.getMaxWeeklyHours()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TeacherPanel.this,
                            "Error loading teachers: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Teacher", true);
        dialog.setSize(400, 270);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField hoursField = new JTextField("18");

        formPanel.add(new JLabel("Full Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Username:"));
        formPanel.add(usernameField);
        formPanel.add(new JLabel("Password:"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel("Max Weekly Hours:"));
        formPanel.add(hoursField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String fullName = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String hoursStr = hoursField.getText().trim();

            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || hoursStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double hours;
            try {
                hours = Double.parseDouble(hoursStr);
                if (hours <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Max weekly hours must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid number for hours.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (userDAO.findByUsername(username) != null) {
                    JOptionPane.showMessageDialog(dialog, "Username '" + username + "' is already taken.", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                teacherDAO.createTeacher(fullName, username, password, hours);
                dialog.dispose();
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showEditDialog() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a teacher to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Teacher teacher = teacherList.get(selectedRow);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Teacher", true);
        dialog.setSize(420, 290);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField(teacher.getFullName());
        JTextField usernameField = new JTextField(teacher.getUsername());
        JPasswordField passwordField = new JPasswordField();
        JTextField hoursField = new JTextField(String.valueOf(teacher.getMaxWeeklyHours()));

        formPanel.add(new JLabel("Full Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Username:"));
        formPanel.add(usernameField);
        formPanel.add(new JLabel("New Password (optional):"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel("Max Weekly Hours:"));
        formPanel.add(hoursField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Update");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String fullName = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String hoursStr = hoursField.getText().trim();

            if (fullName.isEmpty() || username.isEmpty() || hoursStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Full Name, Username and Hours cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double hours;
            try {
                hours = Double.parseDouble(hoursStr);
                if (hours <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Max weekly hours must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid number for hours.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (!username.equalsIgnoreCase(teacher.getUsername())) {
                    User existing = userDAO.findByUsername(username);
                    if (existing != null && existing.getId() != teacher.getUserId()) {
                        JOptionPane.showMessageDialog(dialog, "Username '" + username + "' is already taken.", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                teacherDAO.updateTeacherAndUser(teacher.getId(), teacher.getUserId(), fullName, username, password, hours);
                dialog.dispose();
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteTeacher() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a teacher to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Teacher teacher = teacherList.get(selectedRow);

        User currentUser = Session.getCurrentUser();
        if (currentUser != null && currentUser.getId() == teacher.getUserId()) {
            JOptionPane.showMessageDialog(this,
                    "You cannot delete the teacher account that is currently logged in.",
                    "Delete Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // PART C.4: Check usage in assignments and timetable
        int assignCount = 0;
        int ttCount = 0;
        try {
            assignCount = assignmentDAO.findByTeacherId(teacher.getId()).size();
            ttCount = timetableDAO.getTimetableByTeacher(teacher.getId()).size();
        } catch (SQLException ignored) {}

        StringBuilder msg = new StringBuilder();
        msg.append("Are you sure you want to delete teacher '").append(teacher.getFullName()).append("' (Username: ").append(teacher.getUsername()).append(")?\n\n");
        if (assignCount > 0 || ttCount > 0) {
            msg.append("WARNING: This teacher is currently in use:\n");
            if (assignCount > 0) msg.append("• ").append(assignCount).append(" teaching assignment(s)\n");
            if (ttCount > 0) msg.append("• ").append(ttCount).append(" scheduled timetable period(s)\n");
            msg.append("\nDeleting this teacher will also PERMANENTLY DELETE all of the above!\n\n");
        }
        msg.append("Do you want to proceed?");

        int choice = JOptionPane.showConfirmDialog(this, msg.toString(), "Confirm Delete", JOptionPane.YES_NO_OPTION,
                (assignCount > 0 || ttCount > 0) ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                teacherDAO.deleteTeacherAndUser(teacher.getUserId());
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
