package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AnnouncementReplyRepository;
import com.rainbow.springboot.thymeleafdemo.dao.AnnouncementRepository;
import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dto.AnnouncementView;
import com.rainbow.springboot.thymeleafdemo.entity.Announcement;
import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementCategory;
import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementKind;
import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementReply;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.PermissionCode;

/**
 * The noticeboard.
 *
 * Who sees what:
 *   - managers (super admin or CAN_MANAGE_ANNOUNCEMENTS): everything
 *   - every other employee: all announcements, plus their OWN suggestions and issues
 *     (a suggestion or issue is visible only to its author and to managers)
 *   - reception accounts: Public announcements only, unless they hold CAN_VIEW_ADMIN_ANNOUNCEMENTS
 */
@Service
public class AnnouncementService {

    private static final int PAGE_SIZE = 50;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReplyRepository replyRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final CommunicationAccess access;

    public AnnouncementService(AnnouncementRepository theAnnouncementRepository,
                               AnnouncementReplyRepository theReplyRepository,
                               AppUserRepository theAppUserRepository,
                               NotificationService theNotificationService,
                               CommunicationAccess theAccess) {
        announcementRepository = theAnnouncementRepository;
        replyRepository = theReplyRepository;
        appUserRepository = theAppUserRepository;
        notificationService = theNotificationService;
        access = theAccess;
    }

    /** Whether this user may see this post at all. {@code adminOk}: may read Administration announcements. */
    public boolean canSee(AppUser user, Announcement post, boolean manager, boolean adminOk) {
        if (manager) {
            return true;
        }
        if (post.getAuthor().getId() == user.getId()) {
            return true;
        }
        if (post.getKind() != AnnouncementKind.ANNOUNCEMENT) {
            return false;
        }
        return post.getCategory() != AnnouncementCategory.ADMINISTRATION || adminOk;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementView> board(AppUser user, AnnouncementCategory category) {
        boolean manager = access.isManager(user);
        boolean adminOk = access.canViewAdministration(user);
        List<Announcement> posts = new ArrayList<>();
        for (Announcement post : announcementRepository.findByCategoryOrderByCreatedAtDesc(category, PageRequest.of(0, 150))) {
            if (canSee(user, post, manager, adminOk)) {
                posts.add(post);
            }
            if (posts.size() >= PAGE_SIZE) {
                break;
            }
        }

        Map<Integer, List<AnnouncementReply>> repliesByPost = new HashMap<>();
        if (!posts.isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (Announcement post : posts) {
                ids.add(post.getId());
            }
            for (AnnouncementReply reply : replyRepository.findForAnnouncements(ids)) {
                repliesByPost.computeIfAbsent(reply.getAnnouncement().getId(), k -> new ArrayList<>()).add(reply);
            }
        }

        List<AnnouncementView> views = new ArrayList<>();
        for (Announcement post : posts) {
            boolean canDelete = manager || post.getAuthor().getId() == user.getId();
            views.add(new AnnouncementView(post, repliesByPost.getOrDefault(post.getId(), Collections.emptyList()), canDelete));
        }
        return views;
    }

    @Transactional
    public Announcement post(AppUser user, AnnouncementCategory category, AnnouncementKind kind, String title, String body) {
        String cleanTitle = clean(title, 150, "A title is required.");
        String cleanBody = clean(body, 4000, "A message is required.");
        boolean manager = access.isManager(user);

        AnnouncementCategory finalCategory = category;
        AnnouncementKind finalKind = kind;
        if (manager) {
            finalKind = AnnouncementKind.ANNOUNCEMENT;
        } else {
            if (kind == AnnouncementKind.ANNOUNCEMENT) {
                throw new IllegalStateException("Only management can post announcements. Send a suggestion or an issue instead.");
            }
            finalCategory = AnnouncementCategory.ADMINISTRATION;
        }

        Announcement post = new Announcement();
        post.setAuthor(user);
        post.setCategory(finalCategory);
        post.setKind(finalKind);
        post.setTitle(cleanTitle);
        post.setBody(cleanBody);
        post.setCreatedAt(LocalDateTime.now());
        Announcement saved = announcementRepository.save(post);

        String link = "/announcements?tab=" + (finalCategory == AnnouncementCategory.PUBLIC ? "public" : "administration");
        if (manager) {
            List<AppUser> audience = new ArrayList<>();
            for (AppUser candidate : appUserRepository.findByEnabledTrueOrderByFullNameAsc()) {
                if (candidate.getId() == user.getId()) {
                    continue;
                }
                // Reception accounts are not told about Administration notices they cannot read.
                if (finalCategory == AnnouncementCategory.ADMINISTRATION && !access.canViewAdministration(candidate)) {
                    continue;
                }
                audience.add(candidate);
            }
            notificationService.notifyUsers(audience,
                    "New " + finalCategory.getLabel().toLowerCase() + " announcement: " + shorten(cleanTitle, 120), link);
        } else {
            notificationService.notifyHolders(PermissionCode.CAN_MANAGE_ANNOUNCEMENTS,
                    user.getFullName() + " sent a " + finalKind.getLabel().toLowerCase() + ": " + shorten(cleanTitle, 120),
                    link, user);
        }
        return saved;
    }

    @Transactional
    public void reply(AppUser user, int announcementId, String body) {
        Announcement post = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new IllegalStateException("That post no longer exists."));
        boolean manager = access.isManager(user);
        if (!canSee(user, post, manager, access.canViewAdministration(user))) {
            throw new IllegalStateException("You do not have access to that post.");
        }

        AnnouncementReply reply = new AnnouncementReply();
        reply.setAnnouncement(post);
        reply.setAuthor(user);
        reply.setBody(clean(body, 2000, "Write a reply first."));
        reply.setCreatedAt(LocalDateTime.now());
        replyRepository.save(reply);

        String link = "/announcements?tab=" + (post.getCategory() == AnnouncementCategory.PUBLIC ? "public" : "administration");
        String text = user.getFullName() + " replied to \"" + shorten(post.getTitle(), 80) + "\"";
        if (post.getAuthor().getId() != user.getId()) {
            notificationService.notify(post.getAuthor(), text, link);
        }
        // A staff reply under an official announcement is for management too.
        if (!manager && post.getKind() == AnnouncementKind.ANNOUNCEMENT) {
            notificationService.notifyHolders(PermissionCode.CAN_MANAGE_ANNOUNCEMENTS, text, link, post.getAuthor());
        }
    }

    @Transactional
    public void delete(AppUser user, int announcementId) {
        Announcement post = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new IllegalStateException("That post no longer exists."));
        if (!access.isManager(user) && post.getAuthor().getId() != user.getId()) {
            throw new IllegalStateException("You can only delete your own posts.");
        }
        announcementRepository.delete(post);
    }

    private static String clean(String value, int max, String emptyMessage) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException(emptyMessage);
        }
        if (trimmed.length() > max) {
            throw new IllegalStateException("That is too long (maximum " + max + " characters).");
        }
        return trimmed;
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
