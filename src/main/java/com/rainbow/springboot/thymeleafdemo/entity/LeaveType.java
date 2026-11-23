package com.rainbow.springboot.thymeleafdemo.entity;

/** Kinds of leave an employee can request. */
public enum LeaveType {
    ANNUAL("Annual leave"),
    SICK("Sick leave"),
    UNPAID("Unpaid leave"),
    OTHER("Other leave");

    private final String label;

    LeaveType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
