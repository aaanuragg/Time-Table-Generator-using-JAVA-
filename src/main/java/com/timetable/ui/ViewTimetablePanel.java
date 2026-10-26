package com.timetable.ui;

import com.timetable.dao.ClassDAO;
import com.timetable.dao.RoomDAO;
import com.timetable.dao.TeacherDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.model.*;
import com.timetable.model.DayOfWeek;
import com.timetable.service.TimetableExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ViewTimetablePanel extends JPanel {

    private final TimetableDAO timetableDAO = new TimetableDAO();
    private final ClassDAO classDAO = new ClassDAO();
    private final TeacherDAO teacherDAO = new TeacherDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final TimetableExporter exporter = new TimetableExporter();

    private JComboBox<String> viewTypeCombo;
    private JComboBox<ComboItem> itemCombo;
    private JButton refreshButton;

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statusLabel;

    // Keep a reference to the currently displayed entries and view type
    private List<TimetableEntry> currentEntries = new ArrayList<>();
    private String currentViewType = "Class";
    private String currentItemName = "";

    public ViewTimetablePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
    }

    private void initUI() {
        // Top filter bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));

        JLabel viewByLabel = new JLabel("View By:");
        viewByLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        viewTypeCombo = new JComboBox<>(new String[]{"Class", "Teacher", "Room"});
        viewTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel itemLabel = new JLabel("Select Item:");
        itemLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        itemCombo = new JComboBox<>();
        itemCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemCombo.setPreferredSize(new Dimension(240, 28));

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        topBar.add(viewByLabel);
        topBar.add(viewTypeCombo);
        topBar.add(itemLabel);
        topBar.add(itemCombo);
        topBar.add(refreshButton);

        // Center table grid
        String[] columns = {"Day", "Period 1", "Period 2", "Period 3", "Period 4", "Period 5"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(60);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);

        configureColumnWidths();

        table.setDefaultRenderer(Object.class, new TimetableCellRenderer());

        // Bottom panel with status + export buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        exportPanel.setBorder(BorderFactory.createTitledBorder("Export"));

        JButton exportPdfButton = new JButton("Export PDF");
        exportPdfButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exportPdfButton.addActionListener(e -> exportCurrentPdf());

        JButton exportExcelButton = new JButton("Export Excel");
        exportExcelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exportExcelButton.addActionListener(e -> exportCurrentExcel());

        JButton exportAllButton = new JButton("Export All");
        exportAllButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exportAllButton.addActionListener(e -> exportAll());

        exportPanel.add(exportPdfButton);
        exportPanel.add(exportExcelButton);
        exportPanel.add(exportAllButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(exportPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
        viewTypeCombo.addActionListener(e -> loadItemsForSelectedViewType());
        itemCombo.addActionListener(e -> loadTimetableForSelectedItem());
        refreshButton.addActionListener(e -> loadItemsForSelectedViewType());

        loadItemsForSelectedViewType();
    }

    private void configureColumnWidths() {
        if (table.getColumnModel().getColumnCount() >= 6) {
            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            table.getColumnModel().getColumn(0).setMinWidth(90);
            table.getColumnModel().getColumn(0).setMaxWidth(120);

            for (int i = 1; i <= 5; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(200);
            }
        }
    }

    public void loadData() {
        loadItemsForSelectedViewType();
    }

    private void loadItemsForSelectedViewType() {
        String viewType = (String) viewTypeCombo.getSelectedItem();
        if (viewType == null) return;

        SwingWorker<List<ComboItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ComboItem> doInBackground() throws Exception {
                List<ComboItem> items = new ArrayList<>();
                if ("Class".equals(viewType)) {
                    List<ClassSection> list = classDAO.findAll();
                    for (ClassSection c : list) {
                        items.add(new ComboItem(c.getId(), c.getName()));
                    }
                } else if ("Teacher".equals(viewType)) {
                    List<Teacher> list = teacherDAO.findAll();
                    for (Teacher t : list) {
                        items.add(new ComboItem(t.getId(), t.getFullName()));
                    }
                } else if ("Room".equals(viewType)) {
                    List<Room> list = roomDAO.findAll();
                    for (Room r : list) {
                        String typeSuffix = r.isLab() ? " [LAB]" : " [CLASSROOM]";
                        items.add(new ComboItem(r.getId(), r.getName() + typeSuffix));
                    }
                }
                return items;
            }

            @Override
            protected void done() {
                try {
                    List<ComboItem> items = get();
                    itemCombo.removeAllItems();
                    for (ComboItem item : items) {
                        itemCombo.addItem(item);
                    }
                    if (!items.isEmpty()) {
                        itemCombo.setSelectedIndex(0);
                    } else {
                        tableModel.setRowCount(0);
                        statusLabel.setText("No " + viewType.toLowerCase() + " items found.");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error loading items: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void loadTimetableForSelectedItem() {
        ComboItem selectedItem = (ComboItem) itemCombo.getSelectedItem();
        String viewType = (String) viewTypeCombo.getSelectedItem();

        if (selectedItem == null || viewType == null) {
            return;
        }

        int itemId = selectedItem.id;

        SwingWorker<List<TimetableEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TimetableEntry> doInBackground() throws Exception {
                if ("Class".equals(viewType)) {
                    return timetableDAO.getTimetableByClass(itemId);
                } else if ("Teacher".equals(viewType)) {
                    return timetableDAO.getTimetableByTeacher(itemId);
                } else if ("Room".equals(viewType)) {
                    return timetableDAO.getTimetableByRoom(itemId);
                }
                return Collections.emptyList();
            }

            @Override
            protected void done() {
                try {
                    List<TimetableEntry> entries = get();
                    currentEntries = entries;
                    currentViewType = viewType;
                    currentItemName = selectedItem.label;
                    populateTable(entries, viewType, selectedItem.label);
                } catch (Exception e) {
                    statusLabel.setText("Error loading timetable: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void populateTable(List<TimetableEntry> entries, String viewType, String selectedName) {
        tableModel.setRowCount(0);

        if (entries.isEmpty()) {
            statusLabel.setText("No timetable entries found for " + selectedName +
                    ". (Generate a timetable first from the 'Generate' tab).");
        } else {
            statusLabel.setText("Showing timetable for " + selectedName + " (" + entries.size() + " periods scheduled)");
        }

        Map<String, TimetableEntry> entryMap = new HashMap<>();
        for (TimetableEntry te : entries) {
            entryMap.put(te.getDayOfWeek() + "_" + te.getPeriodNumber(), te);
        }

        DayOfWeek[] days = DayOfWeek.values();
        for (DayOfWeek day : days) {
            Object[] row = new Object[6];
            row[0] = day.toString();

            for (int p = 1; p <= 5; p++) {
                String key = day + "_" + p;
                TimetableEntry te = entryMap.get(key);

                if (te == null) {
                    row[p] = "---";
                } else {
                    String labTag = te.isRequiresLab() ? " [LAB]" : "";

                    if ("Class".equals(viewType)) {
                        row[p] = te.getSubjectName() + labTag + "\n" + te.getTeacherName() + " (" + te.getRoomName() + ")";
                    } else if ("Teacher".equals(viewType)) {
                        row[p] = te.getClassName() + ": " + te.getSubjectName() + labTag + "\n" + te.getRoomName();
                    } else if ("Room".equals(viewType)) {
                        row[p] = te.getClassName() + ": " + te.getSubjectName() + labTag + "\n" + te.getTeacherName();
                    }
                }
            }

            tableModel.addRow(row);
        }

        configureColumnWidths();
    }

    // Export current timetable as PDF
    private void exportCurrentPdf() {
        if (currentEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No timetable to export. Select a class/teacher/room first.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PDF");
        chooser.setSelectedFile(new File(currentItemName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_timetable.pdf"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        try {
            exporter.exportPdf(currentEntries, currentItemName + " Timetable", currentViewType, file);
            JOptionPane.showMessageDialog(this, "PDF exported successfully to:\n" + file.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting PDF: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Export current timetable as Excel
    private void exportCurrentExcel() {
        if (currentEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No timetable to export. Select a class/teacher/room first.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Excel");
        chooser.setSelectedFile(new File(currentItemName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_timetable.xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new File(file.getAbsolutePath() + ".xlsx");
        }

        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        try {
            exporter.exportExcel(currentEntries, currentItemName + " Timetable", currentViewType, file);
            JOptionPane.showMessageDialog(this, "Excel exported successfully to:\n" + file.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting Excel: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Export All: pick a folder, save one PDF per class (and optionally per teacher)
    private void exportAll() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Select Export Folder");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File folder = folderChooser.getSelectedFile();

        JCheckBox teacherCheckbox = new JCheckBox("Also export one PDF per teacher", true);
        int confirm = JOptionPane.showConfirmDialog(this, teacherCheckbox, "Export All Options", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) return;

        boolean includeTeachers = teacherCheckbox.isSelected();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                int count = 0;

                // Export one PDF per class
                List<ClassSection> classes = classDAO.findAll();
                for (ClassSection c : classes) {
                    List<TimetableEntry> entries = timetableDAO.getTimetableByClass(c.getId());
                    if (entries.isEmpty()) continue;

                    String fileName = c.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + "_timetable.pdf";
                    File file = new File(folder, fileName);
                    exporter.exportPdf(entries, c.getName() + " Timetable", "Class", file);
                    count++;
                }

                // Optionally export one PDF per teacher
                if (includeTeachers) {
                    List<Teacher> teachers = teacherDAO.findAll();
                    for (Teacher t : teachers) {
                        List<TimetableEntry> entries = timetableDAO.getTimetableByTeacher(t.getId());
                        if (entries.isEmpty()) continue;

                        String fileName = t.getFullName().replaceAll("[^a-zA-Z0-9_-]", "_") + "_timetable.pdf";
                        File file = new File(folder, fileName);
                        exporter.exportPdf(entries, t.getFullName() + " Timetable", "Teacher", file);
                        count++;
                    }
                }

                return count + " PDF files exported to:\n" + folder.getAbsolutePath();
            }

            @Override
            protected void done() {
                try {
                    String msg = get();
                    JOptionPane.showMessageDialog(ViewTimetablePanel.this, msg, "Export All Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ViewTimetablePanel.this, "Error exporting: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    // Custom Cell Renderer
    private static class TimetableCellRenderer extends DefaultTableCellRenderer {
        private static final Color LAB_BG = new Color(224, 242, 254);
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

    static class ComboItem {
        final int id;
        final String label;

        ComboItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
