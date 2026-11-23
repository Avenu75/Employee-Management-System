package com.rainbow.springboot.thymeleafdemo.dto;

import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.Announcement;
import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementReply;

/** One post plus its replies and what the current viewer may do with it. */
public class AnnouncementView {

    private final Announcement post;
    private final List<AnnouncementReply> replies;
    private final boolean canDelete;

    public AnnouncementView(Announcement post, List<AnnouncementReply> replies, boolean canDelete) {
        this.post = post;
        this.replies = replies;
        this.canDelete = canDelete;
    }

    public Announcement getPost() { return post; }
    public List<AnnouncementReply> getReplies() { return replies; }
    public boolean isCanDelete() { return canDelete; }
}
