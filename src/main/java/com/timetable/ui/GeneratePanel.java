package com.timetable.ui;

import com.timetable.dao.TimetableDAO;
import com.timetable.service.GenerationResult;
import com.timetable.service.TimetableGenerator;
import com.timetable.service.TimetableVerifier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;

public class GeneratePanel extends JPanel {

    private final TimetableDAO timetableDAO = new TimetableDAO();
    private final TimetableGenerator generator = new TimetableGenerator();
    private final TimetableVerifier verifier = new TimetableVerifier();

    private JSpinner timeLimitSpinner;
    private JButton generateButton;
    private JButton verifyButton;
    private JButton clearButton;
    private JProgressBar progressBar;
    private JTextArea resultArea;

    public GeneratePanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        initUI();
    }

    private void initUI() {
        // Controls toolbar
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Timetable Generation & Verification Controls"));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        JLabel spinnerLabel = new JLabel("Time Limit (seconds):");
        spinnerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(20, 5, 60, 1);
        timeLimitSpinner = new JSpinner(spinnerModel);
        timeLimitSpinner.setPreferredSize(new Dimension(65, 28));

        generateButton = new JButton("Generate Timetable");
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        generateButton.addActionListener(e -> onGenerateClick());

        verifyButton = new JButton("Verify Timetable");
        verifyButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        verifyButton.addActionListener(e -> onVerifyClick());

        clearButton = new JButton("Clear Timetable");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.addActionListener(e -> onClearClick());

        controls.add(spinnerLabel);
        controls.add(timeLimitSpinner);
        controls.add(generateButton);
        controls.add(verifyButton);
        controls.add(clearButton);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(100, 12));

        topPanel.add(controls, BorderLayout.CENTER);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        // Output diagnostics area
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Generation Output & Diagnostics"));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setMargin(new Insets(10, 10, 10, 10));
        resultArea.setText("Ready to generate timetable.\nClick 'Generate Timetable' to run the scheduling algorithm, or 'Verify Timetable' to validate existing entries.");

        centerPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void onGenerateClick() {
        try {
            int existingCount = timetableDAO.count();
            if (existingCount > 0) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "An existing timetable was found with " + existingCount + " entries.\n" +
                        "Generating a new schedule will replace all existing entries.\n\n" +
                        "Do you wish to continue?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error checking existing timetable: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int seconds = (Integer) timeLimitSpinner.getValue();
        long timeLimitMillis = seconds * 1000L;

        setRunningState(true);
        resultArea.setText("Generating clash-free timetable (time limit: " + seconds + "s)...\nPlease wait...\n");

        SwingWorker<GenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected GenerationResult doInBackground() throws Exception {
                return generator.generate(timeLimitMillis);
            }

            @Override
            protected void done() {
                setRunningState(false);
                try {
                    GenerationResult res = get();
                    displayResult(res);
                } catch (Exception e) {
                    resultArea.setText("Error occurred during timetable generation:\n" + e.getMessage());
                    JOptionPane.showMessageDialog(GeneratePanel.this,
                            "Generation error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void onVerifyClick() {
        setRunningState(true);
        resultArea.setText("Verifying timetable constraints against database...\nPlease wait...\n");

        SwingWorker<TimetableVerifier.VerificationReport, Void> worker = new SwingWorker<>() {
            @Override
            protected TimetableVerifier.VerificationReport doInBackground() throws Exception {
                return verifier.verify();
            }

            @Override
            protected void done() {
                setRunningState(false);
                try {
                    TimetableVerifier.VerificationReport report = get();
                    resultArea.setText(report.formatReport());
                    resultArea.setCaretPosition(0);
                } catch (Exception e) {
                    resultArea.setText("Error during verification: " + e.getMessage());
                    JOptionPane.showMessageDialog(GeneratePanel.this,
                            "Verification error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void onClearClick() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear all generated timetable entries?\n" +
                "This action cannot be undone.",
                "Confirm Clear Timetable",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                timetableDAO.deleteAll();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    resultArea.setText("Timetable cleared successfully. All timetable entries have been removed.");
                    JOptionPane.showMessageDialog(GeneratePanel.this,
                            "Timetable cleared successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(GeneratePanel.this,
                            "Error clearing timetable: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void displayResult(GenerationResult res) {
        StringBuilder sb = new StringBuilder();
        double seconds = res.getTimeTakenMs() / 1000.0;

        if (res.isSuccess()) {
            sb.append("====================================================\n");
            sb.append(" STATUS: SUCCESS\n");
            sb.append("====================================================\n\n");
            sb.append(res.getMessage()).append("\n\n");
            sb.append("• Total entries created : ").append(res.getEntriesCreated()).append("\n");
            sb.append("• Execution time taken  : ").append(String.format("%.2f", seconds)).append(" seconds\n");
            sb.append("• Same-subject repeats  : ").append(res.getRepeatedSubjectCount()).append(" remaining\n");
            sb.append("• Constraints verified  : 100% clash-free\n");
            sb.append("  - No teacher, class or room double-bookings\n");
            sb.append("  - Teacher unavailability respected\n");
            sb.append("  - Lab subjects scheduled in 2-period consecutive blocks in LAB rooms\n");
            sb.append("  - Theory subjects scheduled in CLASSROOM rooms\n\n");
            sb.append("Switch to 'View Timetable' to inspect the schedule by Class, Teacher, or Room,\n");
            sb.append("or click 'Verify Timetable' above to run a formal audit of all constraints.\n");
        } else {
            sb.append("====================================================\n");
            sb.append(" STATUS: GENERATION FAILED\n");
            sb.append("====================================================\n\n");
            sb.append("Message: ").append(res.getMessage()).append("\n");
            sb.append("Time taken: ").append(String.format("%.2f", seconds)).append(" seconds\n\n");

            if (!res.getValidationErrors().isEmpty()) {
                sb.append("Pre-generation Validation Errors:\n");
                for (String err : res.getValidationErrors()) {
                    sb.append("  - ").append(err).append("\n");
                }
                sb.append("\n");
            }

            if (!res.getBottleneckReasons().isEmpty()) {
                sb.append("Top 3 Likely Causes:\n");
                for (String reason : res.getBottleneckReasons()) {
                    sb.append("  • ").append(reason).append("\n");
                }
                sb.append("\n");
            }

            List<GenerationResult.UnplacedAssignment> unplaced = res.getUnplacedAssignments();
            if (!unplaced.isEmpty()) {
                sb.append("Unplaced Assignments in Best Partial Attempt (").append(unplaced.size()).append("):\n");
                for (GenerationResult.UnplacedAssignment u : unplaced) {
                    sb.append("  • ").append(u.toString()).append("\n");
                }
            }
        }

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }

    private void setRunningState(boolean running) {
        generateButton.setEnabled(!running);
        verifyButton.setEnabled(!running);
        clearButton.setEnabled(!running);
        timeLimitSpinner.setEnabled(!running);
        progressBar.setVisible(running);
    }
}
