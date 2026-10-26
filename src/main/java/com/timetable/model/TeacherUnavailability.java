package com.timetable.model;

public class TeacherUnavailability {
    private int id;
    private int teacherId;
    private int slotId;
    private String reason;

    // Optional convenience reference
    private TimeSlot timeSlot;

    public TeacherUnavailability() {
    }

    public TeacherUnavailability(int teacherId, int slotId, String reason) {
        this.teacherId = teacherId;
        this.slotId = slotId;
        this.reason = reason;
    }

    public TeacherUnavailability(int id, int teacherId, int slotId, String reason) {
        this.id = id;
        this.teacherId = teacherId;
        this.slotId = slotId;
        this.reason = reason;
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

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    @Override
    public String toString() {
        return "Unavailability{" +
                "teacherId=" + teacherId +
                ", slotId=" + slotId +
                ", reason='" + reason + '\'' +
                '}';
    }
}
