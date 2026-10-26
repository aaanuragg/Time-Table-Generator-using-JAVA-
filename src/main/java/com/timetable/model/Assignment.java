package com.timetable.model;

public class Assignment {
    private int id;
    private int teacherId;
    private int subjectId;
    private int classId;
    private int periodsPerWeek;

    // Optional convenience fields populated for UI display
    private String teacherName;
    private String subjectName;
    private String subjectCode;
    private String className;
    private boolean requiresLab;

    public Assignment() {
    }

    public Assignment(int teacherId, int subjectId, int classId, int periodsPerWeek) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.classId = classId;
        this.periodsPerWeek = periodsPerWeek;
    }

    public Assignment(int id, int teacherId, int subjectId, int classId, int periodsPerWeek) {
        this.id = id;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.classId = classId;
        this.periodsPerWeek = periodsPerWeek;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getPeriodsPerWeek() {
        return periodsPerWeek;
    }

    public void setPeriodsPerWeek(int periodsPerWeek) {
        this.periodsPerWeek = periodsPerWeek;
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isRequiresLab() {
        return requiresLab;
    }

    public void setRequiresLab(boolean requiresLab) {
        this.requiresLab = requiresLab;
    }

    @Override
    public String toString() {
        if (teacherName != null && subjectName != null && className != null) {
            String labTag = requiresLab ? " [Lab]" : "";
            return className + " - " + subjectName + labTag + " (" + teacherName + ", " + periodsPerWeek + "p/w)";
        }
        return "Assignment #" + id + " [Teacher=" + teacherId + ", Subject=" + subjectId + ", Class=" + classId + ", Periods=" + periodsPerWeek + "]";
    }
}
