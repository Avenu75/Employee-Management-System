package com.rainbow.springboot.thymeleafdemo.entity;

/** What a post is: an official announcement, or a suggestion / issue sent in by an employee. */
public enum AnnouncementKind {
    ANNOUNCEMENT("Announcement"),
    SUGGESTION("Suggestion"),
    ISSUE("Issue");

    private final String label;

    AnnouncementKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
