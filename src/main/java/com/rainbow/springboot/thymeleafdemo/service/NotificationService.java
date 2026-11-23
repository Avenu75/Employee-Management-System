package com.rainbow.springboot.thymeleafdemo.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.NotificationRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Notification;

/** Creates and reads the in-app notifications shown under the bell in the navbar. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;

    public NotificationService(NotificationRepository theNotificationRepository,
                               AppUserRepository theAppUserRepository) {
        notificationRepository = theNotificationRepository;
        appUserRepository = theAppUserRepository;
    }

    @Transactional
    public void notify(AppUser user, String message, String link) {
        if (user == null) {
            return;
        }
        String text = message.length() > 300 ? message.substring(0, 297) + "..." : message;
        notificationRepository.save(new Notification(user, text, link));
    }

    @Transactional
    public void notifyUsers(Collection<AppUser> users, String message, String link) {
        for (AppUser user : users) {
            notify(user, message, link);
        }
    }

    /** Notifies everyone who holds the permission (and every super admin), except one user. */
    @Transactional
    public void notifyHolders(String permissionCode, String message, String link, AppUser exclude) {
        for (AppUser user : appUserRepository.findNotifiable(permissionCode)) {
            if (exclude != null && user.getId() == exclude.getId()) {
                continue;
            }
            notify(user, message, link);
        }
    }

    @Transactional(readOnly = true)
    public long unreadCount(AppUser user) {
        return user == null ? 0 : notificationRepository.countByUserAndSeenFalse(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(AppUser user) {
        return notificationRepository.findTop50ByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAllSeen(AppUser user) {
        notificationRepository.markAllSeen(user);
    }

    /** Marks one notification as read and returns the page it points to (or null). */
    @Transactional
    public String open(int id, AppUser user) {
        return notificationRepository.findByIdAndUser(id, user).map(n -> {
            n.setSeen(true);
            notificationRepository.save(n);
            return n.getLink();
        }).orElse(null);
    }
}
