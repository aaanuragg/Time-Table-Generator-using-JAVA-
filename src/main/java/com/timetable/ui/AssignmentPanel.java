package com.timetable.ui;

import com.timetable.dao.AssignmentDAO;
import com.timetable.dao.ClassDAO;
import com.timetable.dao.SubjectDAO;
import com.timetable.dao.TeacherDAO;
import com.timetable.model.Assignment;
import com.timetable.model.ClassSection;
import com.timetable.model.Subject;
import com.timetable.model.Teacher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AssignmentPanel extends JPanel {

    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final TeacherDAO teacherDAO = new TeacherDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final ClassDAO classDAO = new ClassDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Assignment> assignmentList = new ArrayList<>();

    public AssignmentPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addButton = new JButton("Add Assignment");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteAssignment());
        refreshButton.addActionListener(e -> loadData());

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);

        // Columns showing names, not raw IDs
        String[] columns = {"ID", "Teacher", "Subject", "Class / Section", "Periods / Week"};
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
        SwingWorker<List<Assignment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Assignment> doInBackground() throws Exception {
                return assignmentDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    assignmentList = get();
                    tableModel.setRowCount(0);
                    for (Assignment a : assignmentList) {
                        tableModel.addRow(new Object[]{
                                a.getId(),
                                a.getTeacherName(),
                                a.getSubjectName(),
                                a.getClassName(),
                                a.getPeriodsPerWeek()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AssignmentPanel.this,
                            "Error loading assignments: " + e.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        List<Teacher> teachers;
        List<Subject> subjects;
        List<ClassSection> classes;

        try {
            teachers = teacherDAO.findAll();
            subjects = subjectDAO.findAll();
            classes = classDAO.findAll();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching dropdown options: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (teachers.isEmpty() || subjects.isEmpty() || classes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please ensure at least one teacher, subject, and class exist before creating assignments.", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Assignment", true);
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JComboBox<Teacher> teacherCombo = new JComboBox<>(new Vector<>(teachers));
        JComboBox<Subject> subjectCombo = new JComboBox<>(new Vector<>(subjects));
        JComboBox<ClassSection> classCombo = new JComboBox<>(new Vector<>(classes));
        JTextField periodsField = new JTextField("4");

        formPanel.add(new JLabel("Teacher:"));
        formPanel.add(teacherCombo);
        formPanel.add(new JLabel("Subject:"));
        formPanel.add(subjectCombo);
        formPanel.add(new JLabel("Class:"));
        formPanel.add(classCombo);
        formPanel.add(new JLabel("Periods / Week:"));
        formPanel.add(periodsField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            Teacher teacher = (Teacher) teacherCombo.getSelectedItem();
            Subject subject = (Subject) subjectCombo.getSelectedItem();
            ClassSection classSection = (ClassSection) classCombo.getSelectedItem();
            String periodsStr = periodsField.getText().trim();

            if (teacher == null || subject == null || classSection == null || periodsStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select all options and enter periods.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int periods;
            try {
                periods = Integer.parseInt(periodsStr);
                if (periods <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Periods per week must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid number of periods.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Check teacher workload limit
                List<Assignment> existing = assignmentDAO.findByTeacherId(teacher.getId());
                int currentTotal = 0;
                for (Assignment a : existing) {
                    currentTotal += a.getPeriodsPerWeek();
                    // Check duplicate teacher + subject + class
                    if (a.getSubjectId() == subject.getId() && a.getClassId() == classSection.getId()) {
                        JOptionPane.showMessageDialog(dialog,
                                "This teacher is already assigned to " + subject.getName() + " for " + classSection.getName() + ".",
                                "Duplicate Assignment", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                if (currentTotal + periods > teacher.getMaxWeeklyHours()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Cannot save assignment:\n" +
                            "Teacher: " + teacher.getFullName() + "\n" +
                            "Max weekly hours: " + teacher.getMaxWeeklyHours() + "\n" +
                            "Currently assigned: " + currentTotal + " periods\n" +
                            "New assignment: " + periods + " periods\n" +
                            "Total would be " + (currentTotal + periods) + " periods, exceeding the limit.",
                            "Workload Limit Exceeded", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                assignmentDAO.insert(new Assignment(teacher.getId(), subject.getId(), classSection.getId(), periods));
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
            JOptionPane.showMessageDialog(this, "Please select an assignment to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Assignment assignment = assignmentList.get(selectedRow);

        List<Teacher> teachers;
        List<Subject> subjects;
        List<ClassSection> classes;

        try {
            teachers = teacherDAO.findAll();
            subjects = subjectDAO.findAll();
            classes = classDAO.findAll();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching dropdown options: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Assignment", true);
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JComboBox<Teacher> teacherCombo = new JComboBox<>(new Vector<>(teachers));
        JComboBox<Subject> subjectCombo = new JComboBox<>(new Vector<>(subjects));
        JComboBox<ClassSection> classCombo = new JComboBox<>(new Vector<>(classes));
        JTextField periodsField = new JTextField(String.valueOf(assignment.getPeriodsPerWeek()));

        // Select current values
        for (Teacher t : teachers) {
            if (t.getId() == assignment.getTeacherId()) {
                teacherCombo.setSelectedItem(t);
                break;
            }
        }
        for (Subject s : subjects) {
            if (s.getId() == assignment.getSubjectId()) {
                subjectCombo.setSelectedItem(s);
                break;
            }
        }
        for (ClassSection c : classes) {
            if (c.getId() == assignment.getClassId()) {
                classCombo.setSelectedItem(c);
                break;
            }
        }

        formPanel.add(new JLabel("Teacher:"));
        formPanel.add(teacherCombo);
        formPanel.add(new JLabel("Subject:"));
        formPanel.add(subjectCombo);
        formPanel.add(new JLabel("Class:"));
        formPanel.add(classCombo);
        formPanel.add(new JLabel("Periods / Week:"));
        formPanel.add(periodsField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Update");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            Teacher teacher = (Teacher) teacherCombo.getSelectedItem();
            Subject subject = (Subject) subjectCombo.getSelectedItem();
            ClassSection classSection = (ClassSection) classCombo.getSelectedItem();
            String periodsStr = periodsField.getText().trim();

            if (teacher == null || subject == null || classSection == null || periodsStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select all options and enter periods.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int periods;
            try {
                periods = Integer.parseInt(periodsStr);
                if (periods <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Periods per week must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid number of periods.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Check teacher workload limit
                List<Assignment> existing = assignmentDAO.findByTeacherId(teacher.getId());
                int currentTotal = 0;
                for (Assignment a : existing) {
                    // Do not count the assignment currently being edited
                    if (a.getId() != assignment.getId()) {
                        currentTotal += a.getPeriodsPerWeek();
                        // Check duplicate teacher + subject + class
                        if (a.getSubjectId() == subject.getId() && a.getClassId() == classSection.getId()) {
                            JOptionPane.showMessageDialog(dialog,
                                    "This teacher is already assigned to " + subject.getName() + " for " + classSection.getName() + ".",
                                    "Duplicate Assignment", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }

                if (currentTotal + periods > teacher.getMaxWeeklyHours()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Cannot update assignment:\n" +
                            "Teacher: " + teacher.getFullName() + "\n" +
                            "Max weekly hours: " + teacher.getMaxWeeklyHours() + "\n" +
                            "Other assigned periods: " + currentTotal + "\n" +
                            "Updated periods: " + periods + "\n" +
                            "Total would be " + (currentTotal + periods) + " periods, exceeding the limit.",
                            "Workload Limit Exceeded", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                assignment.setTeacherId(teacher.getId());
                assignment.setSubjectId(subject.getId());
                assignment.setClassId(classSection.getId());
                assignment.setPeriodsPerWeek(periods);

                assignmentDAO.update(assignment);
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

    private void deleteAssignment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an assignment to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Assignment assignment = assignmentList.get(selectedRow);

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete assignment:\n" +
                assignment.getTeacherName() + " - " + assignment.getSubjectName() + " (" + assignment.getClassName() + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                assignmentDAO.delete(assignment.getId());
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
