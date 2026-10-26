package com.timetable.ui;

import com.timetable.dao.AssignmentDAO;
import com.timetable.dao.ClassDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.model.Assignment;
import com.timetable.model.ClassSection;
import com.timetable.model.TimetableEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClassPanel extends JPanel {

    private final ClassDAO classDAO = new ClassDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final TimetableDAO timetableDAO = new TimetableDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private List<ClassSection> classList = new ArrayList<>();

    public ClassPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addButton = new JButton("Add Class");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteClass());
        refreshButton.addActionListener(e -> loadData());

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);

        String[] columns = {"ID", "Class Name"};
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
        SwingWorker<List<ClassSection>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ClassSection> doInBackground() throws Exception {
                return classDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    classList = get();
                    tableModel.setRowCount(0);
                    for (ClassSection c : classList) {
                        tableModel.addRow(new Object[]{
                                c.getId(),
                                c.getName()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ClassPanel.this,
                            "Error loading classes: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Class", true);
        dialog.setSize(340, 160);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField();
        formPanel.add(new JLabel("Class Name:"));
        formPanel.add(nameField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a class name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (classDAO.findByName(name) != null) {
                    JOptionPane.showMessageDialog(dialog, "Class '" + name + "' already exists.", "Duplicate Class", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                classDAO.insert(new ClassSection(name));
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
            JOptionPane.showMessageDialog(this, "Please select a class to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ClassSection classSection = classList.get(selectedRow);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Class", true);
        dialog.setSize(340, 160);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField(classSection.getName());
        formPanel.add(new JLabel("Class Name:"));
        formPanel.add(nameField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Update");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a class name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (!name.equalsIgnoreCase(classSection.getName())) {
                    ClassSection existing = classDAO.findByName(name);
                    if (existing != null && existing.getId() != classSection.getId()) {
                        JOptionPane.showMessageDialog(dialog, "Class '" + name + "' already exists.", "Duplicate Class", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                classSection.setName(name);
                classDAO.update(classSection);
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

    private void deleteClass() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a class to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ClassSection classSection = classList.get(selectedRow);

        // PART C.4: Check usage in assignments and timetable
        int assignCount = 0;
        int ttCount = 0;
        try {
            assignCount = assignmentDAO.findByClassId(classSection.getId()).size();
            ttCount = timetableDAO.getTimetableByClass(classSection.getId()).size();
        } catch (SQLException ignored) {}

        StringBuilder msg = new StringBuilder();
        msg.append("Are you sure you want to delete class '").append(classSection.getName()).append("'?\n\n");
        if (assignCount > 0 || ttCount > 0) {
            msg.append("WARNING: This class is currently in use:\n");
            if (assignCount > 0) msg.append("• ").append(assignCount).append(" course assignment(s)\n");
            if (ttCount > 0) msg.append("• ").append(ttCount).append(" scheduled timetable period(s)\n");
            msg.append("\nDeleting this class will also PERMANENTLY DELETE all of the above!\n\n");
        }
        msg.append("Do you want to proceed?");

        int choice = JOptionPane.showConfirmDialog(this, msg.toString(), "Confirm Delete", JOptionPane.YES_NO_OPTION,
                (assignCount > 0 || ttCount > 0) ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                classDAO.delete(classSection.getId());
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
