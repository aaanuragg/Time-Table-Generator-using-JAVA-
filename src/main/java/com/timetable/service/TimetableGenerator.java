package com.timetable.service;

import com.timetable.dao.AssignmentDAO;
import com.timetable.dao.ClassDAO;
import com.timetable.dao.RoomDAO;
import com.timetable.dao.TeacherDAO;
import com.timetable.dao.TimeSlotDAO;
import com.timetable.dao.TimetableDAO;
import com.timetable.model.Assignment;
import com.timetable.model.ClassSection;
import com.timetable.model.DayOfWeek;
import com.timetable.model.Room;
import com.timetable.model.Teacher;
import com.timetable.model.TeacherUnavailability;
import com.timetable.model.TimeSlot;
import com.timetable.model.TimetableEntry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TimetableGenerator {

    public static final long DEFAULT_TIME_LIMIT_MS = 20_000L;

    private final TimetableDAO timetableDAO;
    private final AssignmentDAO assignmentDAO;
    private final TeacherDAO teacherDAO;
    private final TimeSlotDAO timeSlotDAO;
    private final RoomDAO roomDAO;
    private final ClassDAO classDAO;

    public TimetableGenerator() {
        this(new TimetableDAO(), new AssignmentDAO(), new TeacherDAO(),
             new TimeSlotDAO(), new RoomDAO(), new ClassDAO());
    }

    public TimetableGenerator(TimetableDAO timetableDAO,
                              AssignmentDAO assignmentDAO,
                              TeacherDAO teacherDAO,
                              TimeSlotDAO timeSlotDAO,
                              RoomDAO roomDAO,
                              ClassDAO classDAO) {
        this.timetableDAO = timetableDAO;
        this.assignmentDAO = assignmentDAO;
        this.teacherDAO = teacherDAO;
        this.timeSlotDAO = timeSlotDAO;
        this.roomDAO = roomDAO;
        this.classDAO = classDAO;
    }

    public GenerationResult generate() throws SQLException {
        return generate(DEFAULT_TIME_LIMIT_MS);
    }

    public GenerationResult generate(long timeLimitMillis) throws SQLException {
        long startTime = System.currentTimeMillis();

        List<Teacher> teachers = teacherDAO.findAll();
        List<Assignment> assignments = assignmentDAO.findAll();
        List<TimeSlot> slots = timeSlotDAO.findAll();
        List<Room> rooms = roomDAO.findAll();
        List<ClassSection> classes = classDAO.findAll();

        if (assignments.isEmpty()) {
            List<String> errors = new ArrayList<>();
            errors.add("No teaching assignments found. Please add assignments in the Assignments tab first.");
            return GenerationResult.validationFailure(errors);
        }

        List<String> validationErrors = validate(teachers, assignments, classes, slots, rooms);
        if (!validationErrors.isEmpty()) {
            return GenerationResult.validationFailure(validationErrors);
        }

        // Separate labs and classrooms
        List<Room> labRooms = new ArrayList<>();
        List<Room> classRooms = new ArrayList<>();
        for (Room r : rooms) {
            if (r.isLab()) {
                labRooms.add(r);
            } else {
                classRooms.add(r);
            }
        }

        if (!classRooms.isEmpty()) {
            classRooms.sort(Comparator.comparingInt(Room::getId));
        }

        boolean hasLabAssignments = false;
        for (Assignment a : assignments) {
            if (a.isRequiresLab()) {
                hasLabAssignments = true;
                break;
            }
        }

        if (hasLabAssignments && labRooms.isEmpty()) {
            List<String> errors = new ArrayList<>();
            errors.add("Assignments require a lab room, but none exist with room_type = 'LAB'. Please add a LAB room.");
            return GenerationResult.validationFailure(errors);
        }

        // Assign one home classroom per class
        Map<Integer, Room> homeRoomMap = new HashMap<>();
        if (!classRooms.isEmpty()) {
            classes.sort(Comparator.comparingInt(ClassSection::getId));
            for (int i = 0; i < classes.size(); i++) {
                Room home = classRooms.get(i % classRooms.size());
                homeRoomMap.put(classes.get(i).getId(), home);
            }
        }

        // Map teacher unavailable slots
        Map<Integer, Set<Integer>> teacherUnavail = new HashMap<>();
        for (Teacher t : teachers) {
            Set<Integer> unavailSet = new HashSet<>();
            List<TeacherUnavailability> list = teacherDAO.getUnavailableSlots(t.getId());
            for (TeacherUnavailability tu : list) {
                unavailSet.add(tu.getSlotId());
            }
            teacherUnavail.put(t.getId(), unavailSet);
        }

        // Map consecutive periods on the same day (period p to period p+1)
        Map<Integer, TimeSlot> nextSlotMap = new HashMap<>();
        for (TimeSlot s1 : slots) {
            for (TimeSlot s2 : slots) {
                if (s1.getDayOfWeek() == s2.getDayOfWeek() && s2.getPeriodNumber() == s1.getPeriodNumber() + 1) {
                    nextSlotMap.put(s1.getId(), s2);
                    break;
                }
            }
        }

        // Split assignments into scheduling units
        List<SchedulingUnit> units = new ArrayList<>();
        int unitId = 1;
        for (Assignment a : assignments) {
            int periods = a.getPeriodsPerWeek();
            if (a.isRequiresLab()) {
                int blocks = periods / 2;
                int remaining = periods % 2;
                for (int i = 0; i < blocks; i++) {
                    units.add(new SchedulingUnit(unitId++, a, 2, true));
                }
                for (int i = 0; i < remaining; i++) {
                    units.add(new SchedulingUnit(unitId++, a, 1, true));
                }
            } else {
                for (int i = 0; i < periods; i++) {
                    units.add(new SchedulingUnit(unitId++, a, 1, false));
                }
            }
        }

        int maxAttempts = 4;
        long perAttemptLimit = Math.max(2000L, timeLimitMillis / maxAttempts);
        long globalDeadline = startTime + timeLimitMillis;

        boolean solved = false;
        long[] seeds = {101L, 202L, 303L};

        GenerationState state = new GenerationState(slots, nextSlotMap, labRooms, classRooms, teacherUnavail, homeRoomMap);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            long attemptDeadline = Math.min(globalDeadline, System.currentTimeMillis() + perAttemptLimit);
            boolean isRetry = (attempt > 0);
            Random rand = isRetry ? new Random(seeds[attempt - 1]) : new Random(42);

            for (SchedulingUnit u : units) {
                u.slot1 = null;
                u.slot2 = null;
                u.room = null;
            }

            state.resetOccupancy();

            solved = backtrack(units, 0, state, attemptDeadline, rand, isRetry);

            if (solved) {
                break;
            }

            if (System.currentTimeMillis() >= globalDeadline) {
                break;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        if (solved) {
            // Improve schedule by moving or swapping repeated subjects
            improvementPass(units, state, slots);

            List<TimetableEntry> entriesToSave = new ArrayList<>();
            for (SchedulingUnit u : units) {
                Assignment a = u.assignment;
                Room r = u.room;

                TimetableEntry e1 = createEntry(a, r, u.slot1);
                entriesToSave.add(e1);

                if (u.slot2 != null) {
                    TimetableEntry e2 = createEntry(a, r, u.slot2);
                    entriesToSave.add(e2);
                }
            }

            timetableDAO.deleteAll();
            for (TimetableEntry entry : entriesToSave) {
                timetableDAO.insert(entry);
            }

            int repeatsLeft = countSameSubjectRepeats(entriesToSave);
            return GenerationResult.success(entriesToSave.size(), entriesToSave, repeatsLeft, totalTime);
        } else {
            // Report only assignments not placed in the best partial attempt
            Map<Integer, Integer> unplacedPeriodsMap = new HashMap<>();
            for (SchedulingUnit u : units) {
                if (!state.bestSnapshot.containsKey(u.id)) {
                    int aId = u.assignment.getId();
                    unplacedPeriodsMap.put(aId, unplacedPeriodsMap.getOrDefault(aId, 0) + u.duration);
                }
            }

            List<GenerationResult.UnplacedAssignment> unplacedList = new ArrayList<>();
            for (Assignment a : assignments) {
                int missing = unplacedPeriodsMap.getOrDefault(a.getId(), 0);
                if (missing > 0) {
                    unplacedList.add(new GenerationResult.UnplacedAssignment(
                            a,
                            missing,
                            "Could not place in clash-free slot within time limit."
                    ));
                }
            }

            List<String> bottleneckReasons = state.getTopBottlenecks(assignments, 3);
            if (bottleneckReasons.size() < 3) {
                for (GenerationResult.UnplacedAssignment ua : unplacedList) {
                    if (bottleneckReasons.size() >= 3) break;
                    Assignment a = ua.getAssignment();
                    if (a.isRequiresLab()) {
                        bottleneckReasons.add("Lab room contention: " + a.getClassName() + " needs 2 consecutive periods in a LAB room for " + a.getSubjectName() + ".");
                    } else {
                        bottleneckReasons.add("Teacher or room conflict: " + a.getTeacherName() + " has tight schedule or conflicting commitments for " + a.getClassName() + ".");
                    }
                }
            }

            String message = "Timetable generation timed out after " + String.format("%.2f", totalTime / 1000.0) + " seconds.";
            return GenerationResult.failure(unplacedList, bottleneckReasons, message, totalTime);
        }
    }

    public List<String> validate(List<Teacher> teachers,
                                 List<Assignment> assignments,
                                 List<ClassSection> classes,
                                 List<TimeSlot> slots,
                                 List<Room> rooms) {
        List<String> errors = new ArrayList<>();

        if (slots == null || slots.isEmpty()) {
            errors.add("No time slots found in database.");
            return errors;
        }

        if (rooms == null || rooms.isEmpty()) {
            errors.add("No rooms found in database.");
            return errors;
        }

        int totalWeeklySlots = slots.size();

        Map<Integer, Integer> teacherAssigned = new HashMap<>();
        for (Assignment a : assignments) {
            teacherAssigned.put(
                    a.getTeacherId(),
                    teacherAssigned.getOrDefault(a.getTeacherId(), 0) + a.getPeriodsPerWeek()
            );
        }

        for (Teacher t : teachers) {
            int assigned = teacherAssigned.getOrDefault(t.getId(), 0);
            if (assigned > t.getMaxWeeklyHours()) {
                errors.add("Teacher '" + t.getFullName() + "' workload exceeded: assigned " + assigned +
                        " periods, max allowed is " + t.getMaxWeeklyHours());
            }
        }

        Map<Integer, Integer> classAssigned = new HashMap<>();
        for (Assignment a : assignments) {
            classAssigned.put(
                    a.getClassId(),
                    classAssigned.getOrDefault(a.getClassId(), 0) + a.getPeriodsPerWeek()
            );
        }

        for (ClassSection c : classes) {
            int assigned = classAssigned.getOrDefault(c.getId(), 0);
            if (assigned > totalWeeklySlots) {
                errors.add("Class '" + c.getName() + "' requires " + assigned +
                        " periods, exceeding total weekly slots (" + totalWeeklySlots + ").");
            }
        }

        return errors;
    }

    // Improvement pass: move or swap repeated theory periods within the same class
    private void improvementPass(List<SchedulingUnit> units, GenerationState state, List<TimeSlot> slots) {
        int maxIterations = 500;

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean improved = false;

            for (SchedulingUnit u1 : units) {
                if (u1.duration != 1 || u1.requiresLab || u1.slot1 == null) continue;

                Assignment a1 = u1.assignment;
                int cId = a1.getClassId();
                int sId1 = a1.getSubjectId();
                DayOfWeek day1 = u1.slot1.getDayOfWeek();

                int count = state.getClassDaySubjectCount(cId, day1, sId1);
                if (count <= 1) continue;

                // Option 1: Move u1 into an open slot on another day
                TimeSlot bestNewSlot = null;
                Room bestNewRoom = null;

                for (TimeSlot candidate : slots) {
                    if (candidate.getDayOfWeek() == day1) continue;

                    // Do not create a duplicate on target day
                    if (state.getClassDaySubjectCount(cId, candidate.getDayOfWeek(), sId1) > 0) continue;

                    int tId1 = a1.getTeacherId();
                    Set<Integer> unavail = state.teacherUnavail.getOrDefault(tId1, Collections.emptySet());
                    if (unavail.contains(candidate.getId())) continue;
                    if (state.isTeacherBusy(tId1, candidate.getId())) continue;
                    if (state.isClassBusy(cId, candidate.getId())) continue;

                    Room chosenRoom = null;
                    Room homeRoom = state.homeRoomMap.get(cId);
                    if (homeRoom != null && !state.isRoomBusy(homeRoom.getId(), candidate.getId())) {
                        chosenRoom = homeRoom;
                    } else {
                        for (Room r : state.classRooms) {
                            if (!state.isRoomBusy(r.getId(), candidate.getId())) {
                                chosenRoom = r;
                                break;
                            }
                        }
                    }
                    if (chosenRoom == null) continue;

                    bestNewSlot = candidate;
                    bestNewRoom = chosenRoom;
                    break;
                }

                if (bestNewSlot != null) {
                    Candidate oldCandidate = new Candidate(u1.slot1, null, u1.room, 0);
                    state.undo(u1, oldCandidate);

                    Candidate newCandidate = new Candidate(bestNewSlot, null, bestNewRoom, 0);
                    state.apply(u1, newCandidate);

                    improved = true;
                    break;
                }

                // Option 2: Swap u1 with another theory unit u2 of the same class on another day
                boolean swapped = false;
                for (SchedulingUnit u2 : units) {
                    if (u2 == u1) continue;
                    if (u2.duration != 1 || u2.requiresLab || u2.slot1 == null) continue;

                    Assignment a2 = u2.assignment;
                    if (a2.getClassId() != cId) continue; // Same class only

                    DayOfWeek day2 = u2.slot1.getDayOfWeek();
                    if (day2 == day1) continue; // Different day only

                    int sId2 = a2.getSubjectId();
                    if (sId2 == sId1) continue; // Different subject only

                    // Ensure neither side gets a new repeat
                    if (state.getClassDaySubjectCount(cId, day2, sId1) > 0) continue;
                    if (state.getClassDaySubjectCount(cId, day1, sId2) > 0) continue;

                    TimeSlot slotA = u1.slot1;
                    TimeSlot slotB = u2.slot1;
                    Room roomA = u1.room;
                    Room roomB = u2.room;

                    int tId1 = a1.getTeacherId();
                    int tId2 = a2.getTeacherId();

                    Candidate cand1 = new Candidate(slotA, null, roomA, 0);
                    Candidate cand2 = new Candidate(slotB, null, roomB, 0);
                    state.undo(u1, cand1);
                    state.undo(u2, cand2);

                    Set<Integer> unavail1 = state.teacherUnavail.getOrDefault(tId1, Collections.emptySet());
                    boolean t1Ok = !unavail1.contains(slotB.getId()) && !state.isTeacherBusy(tId1, slotB.getId());

                    Set<Integer> unavail2 = state.teacherUnavail.getOrDefault(tId2, Collections.emptySet());
                    boolean t2Ok = !unavail2.contains(slotA.getId()) && !state.isTeacherBusy(tId2, slotA.getId());

                    Room newRoom1 = null;
                    Room homeRoom = state.homeRoomMap.get(cId);
                    if (homeRoom != null && !state.isRoomBusy(homeRoom.getId(), slotB.getId())) {
                        newRoom1 = homeRoom;
                    } else if (!state.isRoomBusy(roomB.getId(), slotB.getId())) {
                        newRoom1 = roomB;
                    } else {
                        for (Room r : state.classRooms) {
                            if (!state.isRoomBusy(r.getId(), slotB.getId())) {
                                newRoom1 = r;
                                break;
                            }
                        }
                    }

                    Room newRoom2 = null;
                    if (homeRoom != null && !state.isRoomBusy(homeRoom.getId(), slotA.getId())) {
                        newRoom2 = homeRoom;
                    } else if (!state.isRoomBusy(roomA.getId(), slotA.getId())) {
                        newRoom2 = roomA;
                    } else {
                        for (Room r : state.classRooms) {
                            if (!state.isRoomBusy(r.getId(), slotA.getId())) {
                                newRoom2 = r;
                                break;
                            }
                        }
                    }

                    if (t1Ok && t2Ok && newRoom1 != null && newRoom2 != null) {
                        Candidate newCand1 = new Candidate(slotB, null, newRoom1, 0);
                        Candidate newCand2 = new Candidate(slotA, null, newRoom2, 0);
                        state.apply(u1, newCand1);
                        state.apply(u2, newCand2);
                        swapped = true;
                        improved = true;
                        break;
                    } else {
                        // Undo swap attempt
                        state.apply(u1, cand1);
                        state.apply(u2, cand2);
                    }
                }

                if (swapped) {
                    break;
                }
            }

            if (!improved) {
                break;
            }
        }
    }

    private boolean backtrack(List<SchedulingUnit> units, int placedCount, GenerationState state,
                              long deadline, Random rand, boolean isRetry) {
        if (placedCount == units.size()) {
            return true;
        }

        if (System.currentTimeMillis() > deadline) {
            return false;
        }

        // Most-constrained variable first (MRV)
        SchedulingUnit bestUnit = null;
        List<Candidate> bestCandidates = null;
        int minCandidateCount = Integer.MAX_VALUE;

        for (SchedulingUnit u : units) {
            if (u.slot1 != null) {
                continue;
            }

            List<Candidate> candidates = findCandidates(u, state);

            if (candidates.isEmpty()) {
                state.recordStuck(u.assignment.getId());
                return false;
            }

            if (candidates.size() < minCandidateCount) {
                minCandidateCount = candidates.size();
                bestUnit = u;
                bestCandidates = candidates;
                if (minCandidateCount == 1) {
                    break;
                }
            }
        }

        if (bestUnit == null) {
            return true;
        }

        // Order candidates by penalty score
        if (isRetry) {
            for (Candidate c : bestCandidates) {
                c.effectivePenalty = c.penalty + rand.nextInt(25);
            }
            bestCandidates.sort(Comparator.comparingInt(c -> c.effectivePenalty));
        } else {
            bestCandidates.sort(Comparator.comparingInt(c -> c.penalty));
        }

        for (Candidate c : bestCandidates) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }

            state.apply(bestUnit, c);
            state.checkAndRecordSnapshot(units, placedCount + 1);

            boolean solved = backtrack(units, placedCount + 1, state, deadline, rand, isRetry);
            if (solved) {
                return true;
            }

            state.undo(bestUnit, c);
        }

        return false;
    }

    private List<Candidate> findCandidates(SchedulingUnit u, GenerationState state) {
        List<Candidate> list = new ArrayList<>();
        Assignment a = u.assignment;
        int tId = a.getTeacherId();
        int cId = a.getClassId();

        Set<Integer> unavail = state.teacherUnavail.getOrDefault(tId, Collections.emptySet());
        List<Room> targetRooms = u.requiresLab ? state.labRooms : state.classRooms;

        if (u.duration == 2) {
            for (TimeSlot s1 : state.slots) {
                TimeSlot s2 = state.nextSlotMap.get(s1.getId());
                if (s2 == null) continue;

                if (unavail.contains(s1.getId()) || unavail.contains(s2.getId())) continue;
                if (state.isTeacherBusy(tId, s1.getId()) || state.isTeacherBusy(tId, s2.getId())) continue;
                if (state.isClassBusy(cId, s1.getId()) || state.isClassBusy(cId, s2.getId())) continue;

                Room chosenRoom = null;
                for (Room r : targetRooms) {
                    if (!state.isRoomBusy(r.getId(), s1.getId()) && !state.isRoomBusy(r.getId(), s2.getId())) {
                        chosenRoom = r;
                        break;
                    }
                }
                if (chosenRoom == null) continue;

                int penalty = calculatePenalty(a, s1, state);
                list.add(new Candidate(s1, s2, chosenRoom, penalty));
            }
        } else {
            for (TimeSlot s1 : state.slots) {
                if (unavail.contains(s1.getId())) continue;
                if (state.isTeacherBusy(tId, s1.getId())) continue;
                if (state.isClassBusy(cId, s1.getId())) continue;

                Room chosenRoom = null;
                if (!u.requiresLab) {
                    Room homeRoom = state.homeRoomMap.get(cId);
                    if (homeRoom != null && !state.isRoomBusy(homeRoom.getId(), s1.getId())) {
                        chosenRoom = homeRoom;
                    }
                }

                if (chosenRoom == null) {
                    for (Room r : targetRooms) {
                        if (!state.isRoomBusy(r.getId(), s1.getId())) {
                            chosenRoom = r;
                            break;
                        }
                    }
                }
                if (chosenRoom == null) continue;

                int penalty = calculatePenalty(a, s1, state);
                list.add(new Candidate(s1, null, chosenRoom, penalty));
            }
        }

        return list;
    }

    private int calculatePenalty(Assignment a, TimeSlot slot, GenerationState state) {
        int penalty = 0;
        DayOfWeek day = slot.getDayOfWeek();
        int period = slot.getPeriodNumber();

        int subjCount = state.getClassDaySubjectCount(a.getClassId(), day, a.getSubjectId());
        if (subjCount > 0) {
            penalty += 2000 * subjCount;
        }

        Set<Integer> teacherPeriods = state.getTeacherDayPeriods(a.getTeacherId(), day);
        if (teacherPeriods.contains(period - 1) && teacherPeriods.contains(period - 2)) {
            penalty += 60;
        }

        return penalty;
    }

    public static int countSameSubjectRepeats(List<TimetableEntry> entries) {
        Map<String, Integer> classDaySubj = new HashMap<>();
        int repeats = 0;
        for (TimetableEntry e : entries) {
            if (!e.isRequiresLab()) {
                String key = e.getClassId() + "_" + e.getDayOfWeek() + "_" + e.getSubjectId();
                int count = classDaySubj.getOrDefault(key, 0);
                if (count >= 1) {
                    repeats++;
                }
                classDaySubj.put(key, count + 1);
            }
        }
        return repeats;
    }

    private TimetableEntry createEntry(Assignment a, Room r, TimeSlot s) {
        TimetableEntry e = new TimetableEntry(
                a.getId(),
                a.getClassId(),
                a.getTeacherId(),
                a.getSubjectId(),
                r.getId(),
                s.getId()
        );
        e.setClassName(a.getClassName());
        e.setTeacherName(a.getTeacherName());
        e.setSubjectName(a.getSubjectName());
        e.setSubjectCode(a.getSubjectCode());
        e.setRoomName(r.getName());
        e.setRoomType(r.getRoomType());
        e.setRequiresLab(a.isRequiresLab());
        e.setDayOfWeek(s.getDayOfWeek());
        e.setPeriodNumber(s.getPeriodNumber());
        e.setStartTime(s.getStartTime());
        e.setEndTime(s.getEndTime());
        return e;
    }

    static class SchedulingUnit {
        final int id;
        final Assignment assignment;
        final int duration;
        final boolean requiresLab;

        TimeSlot slot1;
        TimeSlot slot2;
        Room room;

        SchedulingUnit(int id, Assignment assignment, int duration, boolean requiresLab) {
            this.id = id;
            this.assignment = assignment;
            this.duration = duration;
            this.requiresLab = requiresLab;
        }
    }

    static class Candidate {
        final TimeSlot slot1;
        final TimeSlot slot2;
        final Room room;
        final int penalty;
        int effectivePenalty;

        Candidate(TimeSlot slot1, TimeSlot slot2, Room room, int penalty) {
            this.slot1 = slot1;
            this.slot2 = slot2;
            this.room = room;
            this.penalty = penalty;
            this.effectivePenalty = penalty;
        }
    }

    static class UnitPlacement {
        final TimeSlot slot1;
        final TimeSlot slot2;
        final Room room;

        UnitPlacement(TimeSlot slot1, TimeSlot slot2, Room room) {
            this.slot1 = slot1;
            this.slot2 = slot2;
            this.room = room;
        }
    }

    static class GenerationState {
        final List<TimeSlot> slots;
        final Map<Integer, TimeSlot> nextSlotMap;
        final List<Room> labRooms;
        final List<Room> classRooms;
        final Map<Integer, Set<Integer>> teacherUnavail;
        final Map<Integer, Room> homeRoomMap;

        final Set<String> teacherSlots = new HashSet<>();
        final Set<String> classSlots = new HashSet<>();
        final Set<String> roomSlots = new HashSet<>();

        final Map<Integer, Map<DayOfWeek, Map<Integer, Integer>>> classDaySubject = new HashMap<>();
        final Map<Integer, Map<DayOfWeek, Set<Integer>>> teacherDayPeriods = new HashMap<>();

        final Map<Integer, Integer> failureCounts = new HashMap<>();

        // Peak snapshot tracking across search
        final Map<Integer, UnitPlacement> bestSnapshot = new HashMap<>();
        int maxPlaced = 0;

        GenerationState(List<TimeSlot> slots,
                        Map<Integer, TimeSlot> nextSlotMap,
                        List<Room> labRooms,
                        List<Room> classRooms,
                        Map<Integer, Set<Integer>> teacherUnavail,
                        Map<Integer, Room> homeRoomMap) {
            this.slots = slots;
            this.nextSlotMap = nextSlotMap;
            this.labRooms = labRooms;
            this.classRooms = classRooms;
            this.teacherUnavail = teacherUnavail;
            this.homeRoomMap = homeRoomMap;
        }

        void resetOccupancy() {
            teacherSlots.clear();
            classSlots.clear();
            roomSlots.clear();
            classDaySubject.clear();
            teacherDayPeriods.clear();
        }

        void checkAndRecordSnapshot(List<SchedulingUnit> units, int placedCount) {
            if (placedCount > maxPlaced) {
                maxPlaced = placedCount;
                bestSnapshot.clear();
                for (SchedulingUnit u : units) {
                    if (u.slot1 != null) {
                        bestSnapshot.put(u.id, new UnitPlacement(u.slot1, u.slot2, u.room));
                    }
                }
            }
        }

        void recordStuck(int assignmentId) {
            failureCounts.put(assignmentId, failureCounts.getOrDefault(assignmentId, 0) + 1);
        }

        List<String> getTopBottlenecks(List<Assignment> assignments, int limit) {
            Map<Integer, Assignment> map = new HashMap<>();
            for (Assignment a : assignments) map.put(a.getId(), a);

            List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(failureCounts.entrySet());
            sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            List<String> results = new ArrayList<>();
            for (int i = 0; i < sorted.size() && i < limit; i++) {
                int aId = sorted.get(i).getKey();
                int times = sorted.get(i).getValue();
                Assignment a = map.get(aId);
                if (a != null) {
                    results.add(a.getTeacherName() + " teaching " + a.getSubjectName() +
                            " to " + a.getClassName() + " could not find a free slot (stuck " + times + " times during search)");
                }
            }
            return results;
        }

        boolean isTeacherBusy(int teacherId, int slotId) {
            return teacherSlots.contains(teacherId + "_" + slotId);
        }

        boolean isClassBusy(int classId, int slotId) {
            return classSlots.contains(classId + "_" + slotId);
        }

        boolean isRoomBusy(int roomId, int slotId) {
            return roomSlots.contains(roomId + "_" + slotId);
        }

        int getClassDaySubjectCount(int classId, DayOfWeek day, int subjectId) {
            Map<DayOfWeek, Map<Integer, Integer>> dayMap = classDaySubject.get(classId);
            if (dayMap == null) return 0;
            Map<Integer, Integer> subjMap = dayMap.get(day);
            if (subjMap == null) return 0;
            return subjMap.getOrDefault(subjectId, 0);
        }

        Set<Integer> getTeacherDayPeriods(int teacherId, DayOfWeek day) {
            Map<DayOfWeek, Set<Integer>> dayMap = teacherDayPeriods.get(teacherId);
            if (dayMap == null) return Collections.emptySet();
            Set<Integer> periods = dayMap.get(day);
            return periods != null ? periods : Collections.emptySet();
        }

        void apply(SchedulingUnit u, Candidate c) {
            u.slot1 = c.slot1;
            u.slot2 = c.slot2;
            u.room = c.room;

            int tId = u.assignment.getTeacherId();
            int cId = u.assignment.getClassId();
            int sId = u.assignment.getSubjectId();
            int rId = c.room.getId();

            markSlot(tId, cId, rId, sId, c.slot1);
            if (c.slot2 != null) {
                markSlot(tId, cId, rId, sId, c.slot2);
            }
        }

        private void markSlot(int tId, int cId, int rId, int sId, TimeSlot slot) {
            teacherSlots.add(tId + "_" + slot.getId());
            classSlots.add(cId + "_" + slot.getId());
            roomSlots.add(rId + "_" + slot.getId());

            classDaySubject.computeIfAbsent(cId, k -> new EnumMap<>(DayOfWeek.class))
                    .computeIfAbsent(slot.getDayOfWeek(), k -> new HashMap<>())
                    .merge(sId, 1, Integer::sum);

            teacherDayPeriods.computeIfAbsent(tId, k -> new EnumMap<>(DayOfWeek.class))
                    .computeIfAbsent(slot.getDayOfWeek(), k -> new HashSet<>())
                    .add(slot.getPeriodNumber());
        }

        void undo(SchedulingUnit u, Candidate c) {
            int tId = u.assignment.getTeacherId();
            int cId = u.assignment.getClassId();
            int sId = u.assignment.getSubjectId();
            int rId = c.room.getId();

            unmarkSlot(tId, cId, rId, sId, c.slot1);
            if (c.slot2 != null) {
                unmarkSlot(tId, cId, rId, sId, c.slot2);
            }

            u.slot1 = null;
            u.slot2 = null;
            u.room = null;
        }

        private void unmarkSlot(int tId, int cId, int rId, int sId, TimeSlot slot) {
            teacherSlots.remove(tId + "_" + slot.getId());
            classSlots.remove(cId + "_" + slot.getId());
            roomSlots.remove(rId + "_" + slot.getId());

            Map<DayOfWeek, Map<Integer, Integer>> dayMap = classDaySubject.get(cId);
            if (dayMap != null && dayMap.containsKey(slot.getDayOfWeek())) {
                Map<Integer, Integer> subjMap = dayMap.get(slot.getDayOfWeek());
                int cnt = subjMap.getOrDefault(sId, 0);
                if (cnt <= 1) subjMap.remove(sId);
                else subjMap.put(sId, cnt - 1);
            }

            Map<DayOfWeek, Set<Integer>> tPeriods = teacherDayPeriods.get(tId);
            if (tPeriods != null && tPeriods.containsKey(slot.getDayOfWeek())) {
                tPeriods.get(slot.getDayOfWeek()).remove(slot.getPeriodNumber());
            }
        }
    }
}
