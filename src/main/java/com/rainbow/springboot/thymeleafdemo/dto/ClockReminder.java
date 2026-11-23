package com.rainbow.springboot.thymeleafdemo.dto;

/** The banner shown to staff who still need to clock in or out today. */
public class ClockReminder {

    private final String message;
    private final boolean out;

    public ClockReminder(String message, boolean out) {
        this.message = message;
        this.out = out;
    }

    public String getMessage() { return message; }

    /** True when the reminder is to clock OUT (the person is already clocked in). */
    public boolean isOut() { return out; }
}
