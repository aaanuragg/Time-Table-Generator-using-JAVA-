package com.timetable.model;

import java.time.LocalTime;

public class TimeSlot {
    private int id;
    private DayOfWeek dayOfWeek;
    private int periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;

    public TimeSlot() {
    }

    public TimeSlot(DayOfWeek dayOfWeek, int periodNumber, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimeSlot(int id, DayOfWeek dayOfWeek, int periodNumber, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
        return dayOfWeek + " P" + periodNumber + " (" + startTime + " - " + endTime + ")";
    }
}
