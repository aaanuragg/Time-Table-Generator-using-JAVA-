package com.timetable.model;

public class Subject {
    private int id;
    private String code;
    private String name;
    private boolean requiresLab;

    public Subject() {
    }

    public Subject(String code, String name) {
        this.code = code;
        this.name = name;
        this.requiresLab = false;
    }

    public Subject(String code, String name, boolean requiresLab) {
        this.code = code;
        this.name = name;
        this.requiresLab = requiresLab;
    }

    public Subject(int id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.requiresLab = false;
    }

    public Subject(int id, String code, String name, boolean requiresLab) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.requiresLab = requiresLab;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequiresLab() {
        return requiresLab;
    }

    public void setRequiresLab(boolean requiresLab) {
        this.requiresLab = requiresLab;
    }

    @Override
    public String toString() {
        if (requiresLab) {
            return code + " - " + name + " [Lab]";
        }
        return code + " - " + name;
    }
}
