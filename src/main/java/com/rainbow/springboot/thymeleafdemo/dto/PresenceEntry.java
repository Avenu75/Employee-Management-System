package com.rainbow.springboot.thymeleafdemo.dto;

import java.time.LocalTime;

/** One manager in the "who is in the office" list shown in the sidebar. */
public class PresenceEntry {

    private final String name;
    private final LocalTime since;
    private final boolean in;

    public PresenceEntry(String name, LocalTime since, boolean in) {
        this.name = name;
        this.since = since;
        this.in = in;
    }

    public String getName() { return name; }

    /** Clock-in time, or null when the manager has not clocked in today. */
    public LocalTime getSince() { return since; }

    /** True while the manager is clocked in and has not clocked out. */
    public boolean isInOffice() { return in; }

    /** Short status text for the sidebar, e.g. "In since 08:30", "Left" or "Not in yet". */
    public String getLabel() {
        if (in) {
            return "In since " + since;
        }
        return since != null ? "Left" : "Not in yet";
    }
}
