package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

import com.rainbow.springboot.thymeleafdemo.dto.ClockReminder;
import com.rainbow.springboot.thymeleafdemo.dto.PresenceEntry;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.ClockReminderService;
import com.rainbow.springboot.thymeleafdemo.service.ManagementPresenceService;
import com.rainbow.springboot.thymeleafdemo.service.MessageService;
import com.rainbow.springboot.thymeleafdemo.service.NotificationService;

@ControllerAdvice
public class CurrentUserControllerAdvice {

    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final ClockReminderService clockReminderService;
    private final ManagementPresenceService presenceService;

    public CurrentUserControllerAdvice(CurrentUserService theCurrentUserService,
                                       NotificationService theNotificationService,
                                       MessageService theMessageService,
                                       ClockReminderService theClockReminderService,
                                       ManagementPresenceService thePresenceService) {
        currentUserService = theCurrentUserService;
        notificationService = theNotificationService;
        messageService = theMessageService;
        clockReminderService = theClockReminderService;
        presenceService = thePresenceService;
    }

    /** Number shown on the bell icon in the navbar. */
    @ModelAttribute("unreadNotifications")
    public long unreadNotifications() {
        return notificationService.unreadCount(currentUserService.getCurrentUser());
    }

    /** Number shown next to "Messages" in the navbar. */
    @ModelAttribute("unreadMessages")
    public long unreadMessages() {
        return messageService.unreadCount(currentUserService.getCurrentUser());
    }

    /** "Remember to clock in / out" banner; null when nothing is due. */
    @ModelAttribute("clockReminder")
    public ClockReminder clockReminder() {
        return clockReminderService.reminderFor(currentUserService.getCurrentUser());
    }

    /** Which managers have clocked in today, so everyone can see who is in the office. */
    @ModelAttribute("managementPresence")
    public List<PresenceEntry> managementPresence() {
        return presenceService.today();
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        return currentUserService.getCurrentUser();
    }

    @ModelAttribute("currentUserProfileImage")
    public String currentUserProfileImage() {
        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser != null && currentUser.getEmployee() != null
                && currentUser.getEmployee().getProfileImage() != null
                && !currentUser.getEmployee().getProfileImage().isBlank()) {
            return currentUser.getEmployee().getProfileImage();
        }

        return "/images/default-avatar.svg";
    }

    @ModelAttribute("currentUserRoleLabel")
    public String currentUserRoleLabel() {
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            return "Guest";
        }
        if (currentUser.isSuperAdmin()) {
            return "Super Admin";
        }
        if (currentUser.getEmployee() != null && currentUser.getEmployee().getJobTitle() != null
                && !currentUser.getEmployee().getJobTitle().isBlank()) {
            return currentUser.getEmployee().getJobTitle();
        }
        return "Staff";
    }
}
