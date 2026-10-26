package com.timetable.model;

public class Room {
    private int id;
    private String name;
    private int capacity;
    private String roomType = "CLASSROOM";

    public Room() {
    }

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
        this.roomType = "CLASSROOM";
    }

    public Room(String name, int capacity, String roomType) {
        this.name = name;
        this.capacity = capacity;
        this.roomType = (roomType != null) ? roomType : "CLASSROOM";
    }

    public Room(int id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.roomType = "CLASSROOM";
    }

    public Room(int id, String name, int capacity, String roomType) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.roomType = (roomType != null) ? roomType : "CLASSROOM";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isLab() {
        return "LAB".equalsIgnoreCase(roomType);
    }

    @Override
    public String toString() {
        return name + " (" + capacity + " seats, " + roomType + ")";
    }
}
