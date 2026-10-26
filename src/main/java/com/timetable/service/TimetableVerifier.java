package com.timetable.service;

import com.timetable.dao.*;
import com.timetable.model.*;

import java.sql.SQLException;
import java.util.*;

public class TimetableVerifier {

    private final TimetableDAO timetableDAO;
    private final AssignmentDAO assignmentDAO;
    private final TeacherDAO teacherDAO;
    private final RoomDAO roomDAO;
    private final SubjectDAO subjectDAO;

    public TimetableVerifier() {
        this(new TimetableDAO(), new AssignmentDAO(), new TeacherDAO(), new RoomDAO(), new SubjectDAO());
    }

    public TimetableVerifier(TimetableDAO timetableDAO,
                             AssignmentDAO assignmentDAO,
                             TeacherDAO teacherDAO,
                             RoomDAO roomDAO,
                             SubjectDAO subjectDAO) {
        this.timetableDAO = timetableDAO;
        this.assignmentDAO = assignmentDAO;
        this.teacherDAO = teacherDAO;
        this.roomDAO = roomDAO;
        this.subjectDAO = subjectDAO;
    }

    public VerificationReport verify() throws SQLException {
        List<TimetableEntry> entries = timetableDAO.findAll();
        List<Assignment> assignments = assignmentDAO.findAll();
        List<Teacher> teachers = teacherDAO.findAll();
        List<Room> rooms = roomDAO.findAll();
        List<Subject> subjects = subjectDAO.findAll();

        Map<Integer, Room> roomMap = new HashMap<>();
        for (Room r : rooms) roomMap.put(r.getId(), r);

        Map<Integer, Subject> subjectMap = new HashMap<>();
        for (Subject s : subjects) subjectMap.put(s.getId(), s);

        Map<Integer, Teacher> teacherMap = new HashMap<>();
        for (Teacher t : teachers) teacherMap.put(t.getId(), t);

        VerificationReport report = new VerificationReport();

        if (entries.isEmpty()) {
            report.addCheck("Timetable Status", false, "Timetable is completely empty (0 entries).");
            return report;
        }

        // 1. No teacher in two places in the same slot
        Map<String, List<TimetableEntry>> teacherSlotMap = new HashMap<>();
        for (TimetableEntry e : entries) {
            String key = e.getTeacherId() + "_" + e.getSlotId();
            teacherSlotMap.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        int teacherClashes = 0;
        for (List<TimetableEntry> list : teacherSlotMap.values()) {
            if (list.size() > 1) teacherClashes += (list.size() - 1);
        }
        report.addCheck("1. Teacher Clashes", teacherClashes == 0,
                teacherClashes == 0 ? "PASSED (0 clashes found)" : "FAILED (" + teacherClashes + " double-booked slot(s))");

        // 2. No class with two lessons in the same slot
        Map<String, List<TimetableEntry>> classSlotMap = new HashMap<>();
        for (TimetableEntry e : entries) {
            String key = e.getClassId() + "_" + e.getSlotId();
            classSlotMap.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        int classClashes = 0;
        for (List<TimetableEntry> list : classSlotMap.values()) {
            if (list.size() > 1) classClashes += (list.size() - 1);
        }
        report.addCheck("2. Class Clashes", classClashes == 0,
                classClashes == 0 ? "PASSED (0 clashes found)" : "FAILED (" + classClashes + " overlapping lesson(s))");

        // 3. No room used twice in the same slot
        Map<String, List<TimetableEntry>> roomSlotMap = new HashMap<>();
        for (TimetableEntry e : entries) {
            String key = e.getRoomId() + "_" + e.getSlotId();
            roomSlotMap.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        int roomClashes = 0;
        for (List<TimetableEntry> list : roomSlotMap.values()) {
            if (list.size() > 1) roomClashes += (list.size() - 1);
        }
        report.addCheck("3. Room Clashes", roomClashes == 0,
                roomClashes == 0 ? "PASSED (0 clashes found)" : "FAILED (" + roomClashes + " double-booked room(s))");

        // 4. No teacher scheduled in one of their unavailable slots
        int unavailViolations = 0;
        for (Teacher t : teachers) {
            List<TeacherUnavailability> unavails = teacherDAO.getUnavailableSlots(t.getId());
            Set<Integer> unavailSlotIds = new HashSet<>();
            for (TeacherUnavailability tu : unavails) unavailSlotIds.add(tu.getSlotId());

            for (TimetableEntry e : entries) {
                if (e.getTeacherId() == t.getId() && unavailSlotIds.contains(e.getSlotId())) {
                    unavailViolations++;
                }
            }
        }
        report.addCheck("4. Teacher Unavailability", unavailViolations == 0,
                unavailViolations == 0 ? "PASSED (0 violations found)" : "FAILED (" + unavailViolations + " slot(s) scheduled during unavailable hours)");

        // 5. No teacher over max weekly hours
        Map<Integer, Integer> teacherTotalPeriods = new HashMap<>();
        for (TimetableEntry e : entries) {
            teacherTotalPeriods.put(e.getTeacherId(), teacherTotalPeriods.getOrDefault(e.getTeacherId(), 0) + 1);
        }
        int overHourTeachers = 0;
        for (Teacher t : teachers) {
            int scheduled = teacherTotalPeriods.getOrDefault(t.getId(), 0);
            if (scheduled > t.getMaxWeeklyHours()) {
                overHourTeachers++;
            }
        }
        report.addCheck("5. Teacher Max Weekly Hours", overHourTeachers == 0,
                overHourTeachers == 0 ? "PASSED (All teachers within limits)" : "FAILED (" + overHourTeachers + " teacher(s) exceeded max weekly hours)");

        // 6. Every assignment has exactly periods_per_week entries
        Map<Integer, Integer> assignmentCounts = new HashMap<>();
        for (TimetableEntry e : entries) {
            if (e.getAssignmentId() != null) {
                assignmentCounts.put(e.getAssignmentId(), assignmentCounts.getOrDefault(e.getAssignmentId(), 0) + 1);
            }
        }
        int assignmentMismatches = 0;
        for (Assignment a : assignments) {
            int scheduled = assignmentCounts.getOrDefault(a.getId(), 0);
            if (scheduled != a.getPeriodsPerWeek()) {
                assignmentMismatches++;
            }
        }
        report.addCheck("6. Exact Assignment Periods", assignmentMismatches == 0,
                assignmentMismatches == 0 ? "PASSED (" + assignments.size() + "/" + assignments.size() + " assignments match exactly)"
                                          : "FAILED (" + assignmentMismatches + " assignment(s) have period count mismatch)");

        // 7. Every lab subject in LAB room and every theory subject in CLASSROOM
        int roomTypeMismatches = 0;
        for (TimetableEntry e : entries) {
            Subject s = subjectMap.get(e.getSubjectId());
            Room r = roomMap.get(e.getRoomId());
            if (s != null && r != null) {
                if (s.isRequiresLab() && !r.isLab()) {
                    roomTypeMismatches++;
                } else if (!s.isRequiresLab() && r.isLab()) {
                    roomTypeMismatches++;
                }
            }
        }
        report.addCheck("7. Room Type Matching", roomTypeMismatches == 0,
                roomTypeMismatches == 0 ? "PASSED (All lab subjects in LAB, theory in CLASSROOM)"
                                        : "FAILED (" + roomTypeMismatches + " entry/entries assigned to wrong room type)");

        // 8. Every lab block is 2 consecutive periods on the same day (for even periods)
        Map<Integer, List<TimetableEntry>> labEntriesByAssignment = new HashMap<>();
        for (TimetableEntry e : entries) {
            Subject s = subjectMap.get(e.getSubjectId());
            if (s != null && s.isRequiresLab() && e.getAssignmentId() != null) {
                labEntriesByAssignment.computeIfAbsent(e.getAssignmentId(), k -> new ArrayList<>()).add(e);
            }
        }
        int labBlockMismatches = 0;
        int totalLabBlocksChecked = 0;
        for (List<TimetableEntry> list : labEntriesByAssignment.values()) {
            list.sort(Comparator.comparing(TimetableEntry::getDayOfWeek).thenComparingInt(TimetableEntry::getPeriodNumber));
            // Check in pairs
            for (int i = 0; i + 1 < list.size(); i += 2) {
                totalLabBlocksChecked++;
                TimetableEntry e1 = list.get(i);
                TimetableEntry e2 = list.get(i + 1);
                boolean sameDay = (e1.getDayOfWeek() == e2.getDayOfWeek());
                boolean consecutive = (e2.getPeriodNumber() == e1.getPeriodNumber() + 1);
                boolean sameRoom = (e1.getRoomId() == e2.getRoomId());

                if (!sameDay || !consecutive || !sameRoom) {
                    labBlockMismatches++;
                }
            }
        }
        report.addCheck("8. Consecutive Lab Blocks", labBlockMismatches == 0,
                labBlockMismatches == 0 ? "PASSED (" + totalLabBlocksChecked + " lab blocks verified as 2 consecutive periods on same day)"
                                        : "FAILED (" + labBlockMismatches + " lab block(s) not consecutive or split across days/rooms)");

        // 9. Quality Warning: Same theory subject twice on one day for a class
        // (Lab blocks intentionally have 2 periods of the lab subject, so only check theory subjects)
        int sameDayTheoryDuplicates = 0;
        Map<String, Integer> classDayTheorySubjCount = new HashMap<>();
        for (TimetableEntry e : entries) {
            Subject s = subjectMap.get(e.getSubjectId());
            if (s != null && !s.isRequiresLab()) {
                String key = e.getClassId() + "_" + e.getDayOfWeek() + "_" + e.getSubjectId();
                int cnt = classDayTheorySubjCount.getOrDefault(key, 0) + 1;
                classDayTheorySubjCount.put(key, cnt);
                if (cnt == 2) {
                    sameDayTheoryDuplicates++;
                }
            }
        }
        report.addWarning("9. Same Subject Twice per Day",
                sameDayTheoryDuplicates == 0 ? "EXCELLENT (0 duplicate theory subjects on same day)"
                                             : "WARNING (" + sameDayTheoryDuplicates + " instance(s) of same theory subject scheduled twice on the same day)");

        return report;
    }

    public static class VerificationReport {
        private final List<CheckResult> checks = new ArrayList<>();
        private boolean allPassed = true;

        public void addCheck(String name, boolean passed, String detail) {
            checks.add(new CheckResult(name, passed ? "PASSED" : "FAILED", detail, false));
            if (!passed) allPassed = false;
        }

        public void addWarning(String name, String detail) {
            checks.add(new CheckResult(name, "QUALITY NOTE", detail, true));
        }

        public boolean isAllPassed() {
            return allPassed;
        }

        public List<CheckResult> getChecks() {
            return checks;
        }

        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("====================================================\n");
            sb.append(" TIMETABLE VERIFICATION REPORT\n");
            sb.append(" OVERALL RESULT: ").append(allPassed ? "PASSED (100% Clash-Free)" : "FAILED (Issues Found)").append("\n");
            sb.append("====================================================\n\n");

            for (CheckResult c : checks) {
                sb.append(String.format("[%-12s] %-32s : %s\n", c.status, c.name, c.detail));
            }
            sb.append("\n");
            return sb.toString();
        }
    }

    public static class CheckResult {
        public final String name;
        public final String status;
        public final String detail;
        public final boolean isWarning;

        public CheckResult(String name, String status, String detail, boolean isWarning) {
            this.name = name;
            this.status = status;
            this.detail = detail;
            this.isWarning = isWarning;
        }
    }
}
