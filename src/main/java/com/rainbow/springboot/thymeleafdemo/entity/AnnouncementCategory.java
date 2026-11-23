package com.rainbow.springboot.thymeleafdemo.entity;

/** Which noticeboard a post belongs to. */
public enum AnnouncementCategory {
    ADMINISTRATION("Administration"),
    PUBLIC("Public");

    private final String label;

    AnnouncementCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
