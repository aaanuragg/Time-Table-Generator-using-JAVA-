package com.timetable.model;

import java.time.LocalTime;

public class TimetableEntry {
    private int id;
    private Integer assignmentId;
    private int classId;
    private int teacherId;
    private int subjectId;
    private int roomId;
    private int slotId;

    // Optional convenience fields populated for UI display and reporting
    private String className;
    private String teacherName;
    private String subjectName;
    private String subjectCode;
    private String roomName;
    private String roomType;
    private boolean requiresLab;
    private DayOfWeek dayOfWeek;
    private int periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;

    public TimetableEntry() {
    }

    public TimetableEntry(Integer assignmentId, int classId, int teacherId, int subjectId, int roomId, int slotId) {
        this.assignmentId = assignmentId;
        this.classId = classId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.roomId = roomId;
        this.slotId = slotId;
    }

    public TimetableEntry(int id, Integer assignmentId, int classId, int teacherId, int subjectId, int roomId, int slotId) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.classId = classId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.roomId = roomId;
        this.slotId = slotId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isRequiresLab() {
        return requiresLab;
    }

    public void setRequiresLab(boolean requiresLab) {
        this.requiresLab = requiresLab;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(int periodNumber) {
        this.periodNumber = periodNumber;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "TimetableEntry{" +
                "id=" + id +
                ", day=" + dayOfWeek +
                ", period=" + periodNumber +
                ", class='" + className + '\'' +
                ", subject='" + subjectName + '\'' +
                ", teacher='" + teacherName + '\'' +
                ", room='" + roomName + '\'' +
                ", lab=" + requiresLab +
                '}';
    }
}
