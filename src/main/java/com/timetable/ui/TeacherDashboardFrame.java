package com.timetable.ui;

import com.timetable.dao.AssignmentDAO;
import com.timetable.dao.TeacherDAO;
import com.timetable.dao.TimeSlotDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.model.*;
import com.timetable.service.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class TeacherDashboardFrame extends JFrame {

    private final TeacherDAO teacherDAO = new TeacherDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final TimeSlotDAO timeSlotDAO = new TimeSlotDAO();
    private final TimetableDAO timetableDAO = new TimetableDAO();

    private Teacher currentTeacher;
    private int totalAssignedPeriods = 0;

    // Workload tab components
    private JLabel maxHoursLabel;
    private JLabel assignedHoursLabel;
    private JTextField newMaxHoursField;
    private DefaultTableModel workloadTableModel;

    // Unavailability tab components
    private final Map<String, JCheckBox> unavailCheckBoxes = new HashMap<>();
    private final Map<String, TimeSlot> slotLookup = new HashMap<>();
    private final Set<Integer> currentUnavailableSlotIds = new HashSet<>();

    // Timetable tab components
    private JPanel timetableCardPanel;
    private CardLayout timetableCardLayout;
    private DefaultTableModel timetableTableModel;
    private JTable timetableTable;
    private List<TimetableEntry> currentTimetableEntries = new ArrayList<>();
    private final com.timetable.service.TimetableExporter exporter = new com.timetable.service.TimetableExporter();

    public TeacherDashboardFrame() {
        setTitle("Teacher Dashboard - Timetable Generator");
        setSize(1150, 700); // Widen to 1150x700 for full visibility
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadCurrentTeacher();
        initUI();
    }

    private void loadCurrentTeacher() {
        User user = Session.getCurrentUser();
        if (user != null) {
            try {
                currentTeacher = teacherDAO.findByUserId(user.getId());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading teacher profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        User user = Session.getCurrentUser();
        String name = (user != null) ? user.getFullName() : "Teacher";

        JLabel welcomeLabel = new JLabel("Welcome, " + name + " (Teacher)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutButton.addActionListener(e -> logout());

        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        // Center Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        tabbedPane.addTab("My Workload", createWorkloadPanel());
        tabbedPane.addTab("My Unavailability", createUnavailabilityPanel());
        tabbedPane.addTab("My Timetable", createTimetablePanel());

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);

        // Load initial data
        loadWorkloadData();
        loadUnavailabilityData();
        loadTimetableData();
    }

    // -------------------------------------------------------------------------
    // Tab 1: My Workload
    // -------------------------------------------------------------------------
    private JPanel createWorkloadPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBox = new JPanel(new GridLayout(2, 1, 8, 8));
        topBox.setBorder(BorderFactory.createTitledBorder("Workload Summary & Limits"));

        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        maxHoursLabel = new JLabel("Max Weekly Hours: --");
        maxHoursLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        assignedHoursLabel = new JLabel("Total Assigned Periods: --");
        assignedHoursLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        infoRow.add(maxHoursLabel);
        infoRow.add(assignedHoursLabel);

        JPanel editRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        editRow.add(new JLabel("Change Max Weekly Hours:"));
        newMaxHoursField = new JTextField(6);
        JButton saveHoursButton = new JButton("Save New Limit");
        saveHoursButton.addActionListener(e -> saveMaxHours());

        editRow.add(newMaxHoursField);
        editRow.add(saveHoursButton);

        topBox.add(infoRow);
        topBox.add(editRow);

        String[] columns = {"Subject Code", "Subject Name", "Class / Section", "Periods / Week"};
        workloadTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(workloadTableModel);
        table.setRowHeight(28);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        JLabel tableTitle = new JLabel("My Teaching Assignments:");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        centerPanel.add(tableTitle, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.add(topBox, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private void loadWorkloadData() {
        if (currentTeacher == null) return;

        SwingWorker<List<Assignment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Assignment> doInBackground() throws Exception {
                return assignmentDAO.findByTeacherId(currentTeacher.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Assignment> list = get();
                    totalAssignedPeriods = 0;
                    workloadTableModel.setRowCount(0);

                    for (Assignment a : list) {
                        totalAssignedPeriods += a.getPeriodsPerWeek();
                        String code = a.getSubjectCode() != null ? a.getSubjectCode() : "--";
                        String labSuffix = a.isRequiresLab() ? " [Lab]" : "";

                        workloadTableModel.addRow(new Object[]{
                                code,
                                a.getSubjectName() + labSuffix,
                                a.getClassName(),
                                a.getPeriodsPerWeek()
                        });
                    }

                    maxHoursLabel.setText("Max Weekly Hours: " + currentTeacher.getMaxWeeklyHours());
                    assignedHoursLabel.setText("Total Assigned Periods: " + totalAssignedPeriods);
                    newMaxHoursField.setText(String.valueOf(currentTeacher.getMaxWeeklyHours()));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TeacherDashboardFrame.this,
                            "Error loading assignments: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void saveMaxHours() {
        if (currentTeacher == null) return;

        String input = newMaxHoursField.getText().trim();
        double newHours;

        try {
            newHours = Double.parseDouble(input);
            if (newHours <= 0) {
                JOptionPane.showMessageDialog(this, "Max weekly hours must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newHours < totalAssignedPeriods) {
            JOptionPane.showMessageDialog(this,
                    "Max weekly hours (" + newHours + ") cannot be less than your currently assigned periods (" + totalAssignedPeriods + ").",
                    "Invalid Limit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            teacherDAO.updateMaxWeeklyHours(currentTeacher.getId(), newHours);
            currentTeacher.setMaxWeeklyHours(newHours);
            maxHoursLabel.setText("Max Weekly Hours: " + newHours);
            JOptionPane.showMessageDialog(this, "Max weekly hours updated successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Tab 2: My Unavailability
    // -------------------------------------------------------------------------
    private JPanel createUnavailabilityPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("Tick the time slots when you are unavailable to teach, then click 'Save Unavailability':");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        DayOfWeek[] days = DayOfWeek.values();
        int totalDays = days.length;
        int periodsPerDay = 5;

        JPanel gridPanel = new JPanel(new GridLayout(totalDays + 1, periodsPerDay + 1, 8, 8));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JLabel cornerLabel = new JLabel("Day / Period", SwingConstants.CENTER);
        cornerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gridPanel.add(cornerLabel);

        for (int p = 1; p <= periodsPerDay; p++) {
            JLabel pLabel = new JLabel("Period " + p, SwingConstants.CENTER);
            pLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            gridPanel.add(pLabel);
        }

        for (DayOfWeek day : days) {
            JLabel dayLabel = new JLabel(day.toString(), SwingConstants.CENTER);
            dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            gridPanel.add(dayLabel);

            for (int p = 1; p <= periodsPerDay; p++) {
                String key = day + "_" + p;
                JCheckBox checkBox = new JCheckBox("Unavailable");
                checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                unavailCheckBoxes.put(key, checkBox);
                gridPanel.add(checkBox);
            }
        }

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton saveButton = new JButton("Save Unavailability");
        JButton clearAllButton = new JButton("Clear All");

        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.addActionListener(e -> saveUnavailability());

        clearAllButton.addActionListener(e -> {
            for (JCheckBox box : unavailCheckBoxes.values()) {
                box.setSelected(false);
            }
        });

        bottomPanel.add(clearAllButton);
        bottomPanel.add(saveButton);

        panel.add(infoLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadUnavailabilityData() {
        if (currentTeacher == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<TimeSlot> slots = timeSlotDAO.findAll();
                synchronized (slotLookup) {
                    slotLookup.clear();
                    for (TimeSlot s : slots) {
                        slotLookup.put(s.getDayOfWeek() + "_" + s.getPeriodNumber(), s);
                    }
                }

                List<TeacherUnavailability> unavailList = teacherDAO.getUnavailableSlots(currentTeacher.getId());
                currentUnavailableSlotIds.clear();
                for (TeacherUnavailability tu : unavailList) {
                    currentUnavailableSlotIds.add(tu.getSlotId());
                }
                return null;
            }

            @Override
            protected void done() {
                for (Map.Entry<String, JCheckBox> entry : unavailCheckBoxes.entrySet()) {
                    String key = entry.getKey();
                    JCheckBox box = entry.getValue();
                    TimeSlot slot;
                    synchronized (slotLookup) {
                        slot = slotLookup.get(key);
                    }

                    if (slot != null && currentUnavailableSlotIds.contains(slot.getId())) {
                        box.setSelected(true);
                    } else {
                        box.setSelected(false);
                    }
                }
            }
        };
        worker.execute();
    }

    private void saveUnavailability() {
        if (currentTeacher == null) return;

        // PART C.5: Check if the number of unavailable slots makes timetable impossible
        int checkedCount = 0;
        for (JCheckBox box : unavailCheckBoxes.values()) {
            if (box.isSelected()) {
                checkedCount++;
            }
        }

        int totalSlots = unavailCheckBoxes.size(); // 30 slots
        int remainingFreeSlots = totalSlots - checkedCount;

        if (remainingFreeSlots < totalAssignedPeriods) {
            int deficit = totalAssignedPeriods - remainingFreeSlots;
            JOptionPane.showMessageDialog(this,
                    "Warning: You have " + totalAssignedPeriods + " assigned teaching periods per week.\n" +
                    "With " + checkedCount + " unavailable slots selected, only " + remainingFreeSlots + " available slots remain.\n" +
                    "This makes scheduling your classes impossible.\n\n" +
                    "Please uncheck at least " + deficit + " slot(s) before saving.",
                    "Unavailability Limit Exceeded",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (Map.Entry<String, JCheckBox> entry : unavailCheckBoxes.entrySet()) {
                    String key = entry.getKey();
                    JCheckBox box = entry.getValue();
                    TimeSlot slot;
                    synchronized (slotLookup) {
                        slot = slotLookup.get(key);
                    }

                    if (slot == null) continue;

                    boolean isChecked = box.isSelected();
                    boolean wasMarked = currentUnavailableSlotIds.contains(slot.getId());

                    if (isChecked && !wasMarked) {
                        teacherDAO.addUnavailableSlot(currentTeacher.getId(), slot.getId(), "Unavailable");
                        currentUnavailableSlotIds.add(slot.getId());
                    } else if (!isChecked && wasMarked) {
                        teacherDAO.removeUnavailableSlot(currentTeacher.getId(), slot.getId());
                        currentUnavailableSlotIds.remove(slot.getId());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                JOptionPane.showMessageDialog(TeacherDashboardFrame.this,
                        "Unavailability preferences saved successfully!");
                loadTimetableData();
            }
        };
        worker.execute();
    }

    // -------------------------------------------------------------------------
    // Tab 3: My Timetable
    // -------------------------------------------------------------------------
    private JPanel createTimetablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton refreshButton = new JButton("Refresh Timetable");
        refreshButton.addActionListener(e -> loadTimetableData());
        topBar.add(refreshButton);

        JButton exportPdfButton = new JButton("Export PDF");
        exportPdfButton.addActionListener(e -> exportTimetablePdf());
        topBar.add(exportPdfButton);

        timetableCardLayout = new CardLayout();
        timetableCardPanel = new JPanel(timetableCardLayout);

        // Card 1: Empty state label
        JLabel emptyLabel = new JLabel("No timetable generated yet. Please contact the administrator.", SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        emptyLabel.setForeground(Color.GRAY);
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);

        // Card 2: Table grid
        String[] columns = {"Day", "Period 1", "Period 2", "Period 3", "Period 4", "Period 5"};
        timetableTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        timetableTable = new JTable(timetableTableModel);
        timetableTable.setRowHeight(60); // PART A.1: row height 60
        timetableTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timetableTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        timetableTable.getTableHeader().setReorderingAllowed(false);

        configureTimetableColumnWidths();

        // Custom renderer for highlighting lab sessions and unavailable slots
        timetableTable.setDefaultRenderer(Object.class, new TeacherTimetableCellRenderer());

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(new JScrollPane(timetableTable), BorderLayout.CENTER);

        timetableCardPanel.add(emptyPanel, "EMPTY");
        timetableCardPanel.add(tablePanel, "TABLE");

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(timetableCardPanel, BorderLayout.CENTER);

        return panel;
    }

    private void configureTimetableColumnWidths() {
        if (timetableTable != null && timetableTable.getColumnModel().getColumnCount() >= 6) {
            timetableTable.getColumnModel().getColumn(0).setPreferredWidth(100);
            timetableTable.getColumnModel().getColumn(0).setMinWidth(90);
            timetableTable.getColumnModel().getColumn(0).setMaxWidth(120);

            for (int i = 1; i <= 5; i++) {
                timetableTable.getColumnModel().getColumn(i).setPreferredWidth(200);
            }
        }
    }

    private void loadTimetableData() {
        if (currentTeacher == null) return;

        class LoadedData {
            List<TimetableEntry> entries;
            Map<String, TimeSlot> slotsMap = new HashMap<>();
            Set<Integer> unavailSet = new HashSet<>();
        }

        SwingWorker<LoadedData, Void> worker = new SwingWorker<>() {
            @Override
            protected LoadedData doInBackground() throws Exception {
                LoadedData data = new LoadedData();

                // Guarantee slots are loaded
                List<TimeSlot> slots = timeSlotDAO.findAll();
                for (TimeSlot s : slots) {
                    data.slotsMap.put(s.getDayOfWeek() + "_" + s.getPeriodNumber(), s);
                }

                synchronized (slotLookup) {
                    slotLookup.clear();
                    slotLookup.putAll(data.slotsMap);
                }

                // Fetch unavailable slots
                List<TeacherUnavailability> unavails = teacherDAO.getUnavailableSlots(currentTeacher.getId());
                for (TeacherUnavailability tu : unavails) {
                    data.unavailSet.add(tu.getSlotId());
                }

                // Fetch timetable entries
                data.entries = timetableDAO.getTimetableByTeacher(currentTeacher.getId());

                return data;
            }

            @Override
            protected void done() {
                try {
                    LoadedData data = get();
                    currentTimetableEntries = data.entries;

                    if (data.entries.isEmpty()) {
                        timetableCardLayout.show(timetableCardPanel, "EMPTY");
                    } else {
                        timetableTableModel.setRowCount(0);

                        Map<String, TimetableEntry> entryMap = new HashMap<>();
                        for (TimetableEntry te : data.entries) {
                            entryMap.put(te.getDayOfWeek() + "_" + te.getPeriodNumber(), te);
                        }

                        DayOfWeek[] days = DayOfWeek.values();
                        for (DayOfWeek day : days) {
                            Object[] row = new Object[6];
                            row[0] = day.toString();

                            for (int p = 1; p <= 5; p++) {
                                String key = day + "_" + p;
                                if (entryMap.containsKey(key)) {
                                    TimetableEntry te = entryMap.get(key);
                                    String labTag = te.isRequiresLab() ? " [LAB]" : "";
                                    row[p] = te.getClassName() + ": " + te.getSubjectName() + labTag + "\n" + te.getRoomName();
                                } else {
                                    TimeSlot s = data.slotsMap.get(key);
                                    if (s != null && data.unavailSet.contains(s.getId())) {
                                        row[p] = "[Unavailable]";
                                    } else {
                                        row[p] = "---";
                                    }
                                }
                            }
                            timetableTableModel.addRow(row);
                        }

                        configureTimetableColumnWidths();
                        timetableCardLayout.show(timetableCardPanel, "TABLE");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TeacherDashboardFrame.this,
                            "Error loading timetable: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void exportTimetablePdf() {
        if (currentTimetableEntries == null || currentTimetableEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No timetable to export.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String teacherName = (currentTeacher != null) ? currentTeacher.getFullName() : "Teacher";
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Timetable PDF");
        chooser.setSelectedFile(new java.io.File(teacherName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_timetable.pdf"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new java.io.File(file.getAbsolutePath() + ".pdf");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        try {
            exporter.exportPdf(currentTimetableEntries, teacherName + " Timetable", "Teacher", file);
            JOptionPane.showMessageDialog(this, "PDF exported successfully to:\n" + file.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exporting PDF: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        Session.clear();
        dispose();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
    }

    // Custom Table Cell Renderer for Teacher Timetable
    private static class TeacherTimetableCellRenderer extends DefaultTableCellRenderer {
        private static final Color LAB_BG = new Color(224, 242, 254);
        private static final Color UNAVAIL_BG = new Color(254, 226, 226);
        private static final Color UNAVAIL_FG = new Color(185, 28, 28);
        private static final Color EMPTY_FG = Color.GRAY;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0) {
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(table.getTableHeader().getBackground());
                    c.setForeground(table.getTableHeader().getForeground());
                }
                return c;
            }

            setHorizontalAlignment(SwingConstants.CENTER);
            String text = (value != null) ? value.toString() : "";

            if (!isSelected) {
                if (text.contains("[LAB]")) {
                    c.setBackground(LAB_BG);
                    c.setForeground(new Color(3, 105, 161));
                    setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else if ("[Unavailable]".equals(text)) {
                    c.setBackground(UNAVAIL_BG);
                    c.setForeground(UNAVAIL_FG);
                    setFont(new Font("Segoe UI", Font.ITALIC, 11));
                } else if ("---".equals(text)) {
                    c.setBackground(table.getBackground());
                    c.setForeground(EMPTY_FG);
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                    setFont(new Font("Segoe UI", Font.PLAIN, 11));
                }
            }

            if (text.contains("\n")) {
                String html = "<html><center>" + text.replace("\n", "<br/>") + "</center></html>";
                setText(html);
            }

            return c;
        }
    }
}
