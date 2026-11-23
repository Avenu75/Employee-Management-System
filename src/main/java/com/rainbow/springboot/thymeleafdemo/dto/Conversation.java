package com.rainbow.springboot.thymeleafdemo.dto;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.PrivateMessage;

/** One line in the inbox: the other person, the latest message and how many are unread. */
public class Conversation {

    private final AppUser other;
    private final PrivateMessage last;
    private final long unread;

    public Conversation(AppUser other, PrivateMessage last, long unread) {
        this.other = other;
        this.last = last;
        this.unread = unread;
    }

    public AppUser getOther() { return other; }
    public PrivateMessage getLast() { return last; }
    public long getUnread() { return unread; }

    /** True when the latest message was sent by the other person. */
    public boolean isLastFromOther() {
        return last.getSender().getId() == other.getId();
    }
}
