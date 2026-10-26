package com.timetable.ui;

import com.timetable.dao.AssignmentDAO;
import com.timetable.dao.SubjectDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.model.Assignment;
import com.timetable.model.Subject;
import com.timetable.model.TimetableEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SubjectPanel extends JPanel {

    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final TimetableDAO timetableDAO = new TimetableDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Subject> subjectList = new ArrayList<>();

    public SubjectPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addButton = new JButton("Add Subject");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteSubject());
        refreshButton.addActionListener(e -> loadData());

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);

        String[] columns = {"ID", "Code", "Subject Name", "Requires Lab"};
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
        SwingWorker<List<Subject>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Subject> doInBackground() throws Exception {
                return subjectDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    subjectList = get();
                    tableModel.setRowCount(0);
                    for (Subject s : subjectList) {
                        tableModel.addRow(new Object[]{
                                s.getId(),
                                s.getCode(),
                                s.getName(),
                                s.isRequiresLab() ? "Yes" : "No"
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SubjectPanel.this,
                            "Error loading subjects: " + e.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Subject", true);
        dialog.setSize(380, 240);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField codeField = new JTextField();
        JTextField nameField = new JTextField();
        JCheckBox labCheckBox = new JCheckBox("Requires Lab Room");

        formPanel.add(new JLabel("Subject Code:"));
        formPanel.add(codeField);
        formPanel.add(new JLabel("Subject Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Lab Subject:"));
        formPanel.add(labCheckBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();

            if (code.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in code and name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (subjectDAO.findByCode(code) != null) {
                    JOptionPane.showMessageDialog(dialog, "Subject code '" + code + "' already exists.", "Duplicate Code", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                subjectDAO.insert(new Subject(code, name, labCheckBox.isSelected()));
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
            JOptionPane.showMessageDialog(this, "Please select a subject to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Subject subject = subjectList.get(selectedRow);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Subject", true);
        dialog.setSize(380, 240);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField codeField = new JTextField(subject.getCode());
        JTextField nameField = new JTextField(subject.getName());
        JCheckBox labCheckBox = new JCheckBox("Requires Lab Room", subject.isRequiresLab());

        formPanel.add(new JLabel("Subject Code:"));
        formPanel.add(codeField);
        formPanel.add(new JLabel("Subject Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Lab Subject:"));
        formPanel.add(labCheckBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Update");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();

            if (code.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in code and name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (!code.equalsIgnoreCase(subject.getCode())) {
                    Subject existing = subjectDAO.findByCode(code);
                    if (existing != null && existing.getId() != subject.getId()) {
                        JOptionPane.showMessageDialog(dialog, "Subject code '" + code + "' already exists.", "Duplicate Code", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                subject.setCode(code);
                subject.setName(name);
                subject.setRequiresLab(labCheckBox.isSelected());
                subjectDAO.update(subject);
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

    private void deleteSubject() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a subject to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Subject subject = subjectList.get(selectedRow);

        // PART C.4: Check usage in assignments and timetable
        int assignCount = 0;
        int ttCount = 0;
        try {
            for (Assignment a : assignmentDAO.findAll()) {
                if (a.getSubjectId() == subject.getId()) assignCount++;
            }
            for (TimetableEntry te : timetableDAO.findAll()) {
                if (te.getSubjectId() == subject.getId()) ttCount++;
            }
        } catch (SQLException ignored) {}

        StringBuilder msg = new StringBuilder();
        msg.append("Are you sure you want to delete subject '").append(subject.getName()).append("' (").append(subject.getCode()).append(")?\n\n");
        if (assignCount > 0 || ttCount > 0) {
            msg.append("WARNING: This subject is currently in use:\n");
            if (assignCount > 0) msg.append("• ").append(assignCount).append(" course assignment(s)\n");
            if (ttCount > 0) msg.append("• ").append(ttCount).append(" scheduled timetable period(s)\n");
            msg.append("\nDeleting this subject will also PERMANENTLY DELETE all of the above!\n\n");
        }
        msg.append("Do you want to proceed?");

        int choice = JOptionPane.showConfirmDialog(this, msg.toString(), "Confirm Delete", JOptionPane.YES_NO_OPTION,
                (assignCount > 0 || ttCount > 0) ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                subjectDAO.delete(subject.getId());
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
