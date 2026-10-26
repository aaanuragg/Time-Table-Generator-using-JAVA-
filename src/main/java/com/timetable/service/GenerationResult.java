package com.timetable.service;

import com.timetable.model.Assignment;
import com.timetable.model.TimetableEntry;

import java.util.Collections;
import java.util.List;

public class GenerationResult {

    private final boolean success;
    private final int entriesCreated;
    private final List<UnplacedAssignment> unplacedAssignments;
    private final List<String> validationErrors;
    private final List<String> bottleneckReasons;
    private final String message;
    private final List<TimetableEntry> entries;
    private final int repeatedSubjectCount;
    private long timeTakenMs = 0;

    public GenerationResult(boolean success,
                            int entriesCreated,
                            List<UnplacedAssignment> unplacedAssignments,
                            List<String> validationErrors,
                            List<String> bottleneckReasons,
                            String message,
                            List<TimetableEntry> entries) {
        this(success, entriesCreated, unplacedAssignments, validationErrors, bottleneckReasons, message, entries, 0);
    }

    public GenerationResult(boolean success,
                            int entriesCreated,
                            List<UnplacedAssignment> unplacedAssignments,
                            List<String> validationErrors,
                            List<String> bottleneckReasons,
                            String message,
                            List<TimetableEntry> entries,
                            int repeatedSubjectCount) {
        this.success = success;
        this.entriesCreated = entriesCreated;
        this.unplacedAssignments = unplacedAssignments != null ? unplacedAssignments : Collections.emptyList();
        this.validationErrors = validationErrors != null ? validationErrors : Collections.emptyList();
        this.bottleneckReasons = bottleneckReasons != null ? bottleneckReasons : Collections.emptyList();
        this.message = message != null ? message : "";
        this.entries = entries != null ? entries : Collections.emptyList();
        this.repeatedSubjectCount = repeatedSubjectCount;
    }

    public static GenerationResult success(int entriesCreated, List<TimetableEntry> entries, int repeatedSubjects, long timeTakenMs) {
        String repeatMsg = (repeatedSubjects == 0) ? "0 same-subject repeats remaining"
                                                   : repeatedSubjects + " same-subject repeat(s) remaining";
        String msg = "Timetable generated successfully with " + entriesCreated + " clash-free entries (" + repeatMsg + ").";

        GenerationResult res = new GenerationResult(
                true,
                entriesCreated,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                msg,
                entries,
                repeatedSubjects
        );
        res.setTimeTakenMs(timeTakenMs);
        return res;
    }

    public static GenerationResult success(int entriesCreated, List<TimetableEntry> entries, long timeTakenMs) {
        return success(entriesCreated, entries, 0, timeTakenMs);
    }

    public static GenerationResult failure(List<UnplacedAssignment> unplacedAssignments,
                                           List<String> bottleneckReasons,
                                           String message,
                                           long timeTakenMs) {
        GenerationResult res = new GenerationResult(
                false,
                0,
                unplacedAssignments,
                Collections.emptyList(),
                bottleneckReasons,
                message,
                Collections.emptyList(),
                0
        );
        res.setTimeTakenMs(timeTakenMs);
        return res;
    }

    public static GenerationResult validationFailure(List<String> validationErrors) {
        return new GenerationResult(
                false,
                0,
                Collections.emptyList(),
                validationErrors,
                Collections.emptyList(),
                "Pre-generation validation failed with " + validationErrors.size() + " error(s).",
                Collections.emptyList(),
                0
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public int getEntriesCreated() {
        return entriesCreated;
    }

    public List<UnplacedAssignment> getUnplacedAssignments() {
        return Collections.unmodifiableList(unplacedAssignments);
    }

    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    public List<String> getBottleneckReasons() {
        return Collections.unmodifiableList(bottleneckReasons);
    }

    public String getMessage() {
        return message;
    }

    public List<TimetableEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int getRepeatedSubjectCount() {
        return repeatedSubjectCount;
    }

    public long getTimeTakenMs() {
        return timeTakenMs;
    }

    public void setTimeTakenMs(long timeTakenMs) {
        this.timeTakenMs = timeTakenMs;
    }

    public static class UnplacedAssignment {
        private final Assignment assignment;
        private final int unplacedPeriods;
        private final String reason;

        public UnplacedAssignment(Assignment assignment, int unplacedPeriods, String reason) {
            this.assignment = assignment;
            this.unplacedPeriods = unplacedPeriods;
            this.reason = reason;
        }

        public Assignment getAssignment() {
            return assignment;
        }

        public int getUnplacedPeriods() {
            return unplacedPeriods;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            String teacher = assignment != null ? assignment.getTeacherName() : "Unknown";
            String subject = assignment != null ? assignment.getSubjectName() : "Unknown";
            String className = assignment != null ? assignment.getClassName() : "Unknown";
            int total = assignment != null ? assignment.getPeriodsPerWeek() : unplacedPeriods;
            return String.format("%s - %s for %s: %d of %d period(s) unplaced (%s)",
                    teacher, subject, className, unplacedPeriods, total, reason);
        }
    }
}
