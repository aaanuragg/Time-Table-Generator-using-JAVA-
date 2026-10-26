package com.timetable.ui;

import com.timetable.dao.RoomDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.model.Room;
import com.timetable.model.TimetableEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomPanel extends JPanel {

    private final RoomDAO roomDAO = new RoomDAO();
    private final TimetableDAO timetableDAO = new TimetableDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Room> roomList = new ArrayList<>();

    public RoomPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadData();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addButton = new JButton("Add Room");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteRoom());
        refreshButton.addActionListener(e -> loadData());

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);

        String[] columns = {"ID", "Room Name", "Capacity", "Type"};
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
        SwingWorker<List<Room>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Room> doInBackground() throws Exception {
                return roomDAO.findAll();
            }

            @Override
            protected void done() {
                try {
                    roomList = get();
                    tableModel.setRowCount(0);
                    for (Room r : roomList) {
                        tableModel.addRow(new Object[]{
                                r.getId(),
                                r.getName(),
                                r.getCapacity(),
                                r.getRoomType()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RoomPanel.this,
                            "Error loading rooms: " + e.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Room", true);
        dialog.setSize(360, 240);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField();
        JTextField capField = new JTextField("40");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"CLASSROOM", "LAB"});

        formPanel.add(new JLabel("Room Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Capacity:"));
        formPanel.add(capField);
        formPanel.add(new JLabel("Room Type:"));
        formPanel.add(typeCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String capStr = capField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();

            if (name.isEmpty() || capStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int capacity;
            try {
                capacity = Integer.parseInt(capStr);
                if (capacity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Capacity must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid integer for capacity.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (roomDAO.findByName(name) != null) {
                    JOptionPane.showMessageDialog(dialog, "Room '" + name + "' already exists.", "Duplicate Room", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                roomDAO.insert(new Room(name, capacity, type));
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
            JOptionPane.showMessageDialog(this, "Please select a room to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Room room = roomList.get(selectedRow);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Room", true);
        dialog.setSize(360, 240);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField nameField = new JTextField(room.getName());
        JTextField capField = new JTextField(String.valueOf(room.getCapacity()));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"CLASSROOM", "LAB"});
        typeCombo.setSelectedItem(room.getRoomType());

        formPanel.add(new JLabel("Room Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Capacity:"));
        formPanel.add(capField);
        formPanel.add(new JLabel("Room Type:"));
        formPanel.add(typeCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Update");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String capStr = capField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();

            if (name.isEmpty() || capStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int capacity;
            try {
                capacity = Integer.parseInt(capStr);
                if (capacity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Capacity must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid integer for capacity.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (!name.equalsIgnoreCase(room.getName())) {
                    Room existing = roomDAO.findByName(name);
                    if (existing != null && existing.getId() != room.getId()) {
                        JOptionPane.showMessageDialog(dialog, "Room '" + name + "' already exists.", "Duplicate Room", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                room.setName(name);
                room.setCapacity(capacity);
                room.setRoomType(type);
                roomDAO.update(room);
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

    private void deleteRoom() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a room to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Room room = roomList.get(selectedRow);

        // PART C.4: Check usage in timetable
        int ttCount = 0;
        try {
            ttCount = timetableDAO.getTimetableByRoom(room.getId()).size();
        } catch (SQLException ignored) {}

        StringBuilder msg = new StringBuilder();
        msg.append("Are you sure you want to delete room '").append(room.getName()).append("'?\n\n");
        if (ttCount > 0) {
            msg.append("WARNING: This room is currently in use:\n");
            msg.append("• ").append(ttCount).append(" scheduled timetable period(s)\n");
            msg.append("\nDeleting this room will also PERMANENTLY DELETE all of these timetable entries!\n\n");
        }
        msg.append("Do you want to proceed?");

        int choice = JOptionPane.showConfirmDialog(this, msg.toString(), "Confirm Delete", JOptionPane.YES_NO_OPTION,
                (ttCount > 0) ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                roomDAO.delete(room.getId());
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
