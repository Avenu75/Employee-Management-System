package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.NotificationService;

@Controller
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService theNotificationService, CurrentUserService theCurrentUserService) {
        notificationService = theNotificationService;
        currentUserService = theCurrentUserService;
    }

    @GetMapping
    public String list(Model model) {
        AppUser user = currentUserService.getCurrentUser();
        model.addAttribute("notifications", user != null ? notificationService.recent(user) : java.util.List.of());
        return "notifications";
    }

    /** Marks one notification read, then goes to the page it points to. */
    @PostMapping("/{id}/open")
    public String open(@PathVariable int id) {
        AppUser user = currentUserService.getCurrentUser();
        String link = user != null ? notificationService.open(id, user) : null;
        // Only ever redirect inside this application.
        if (link != null && link.startsWith("/") && !link.startsWith("//")) {
            return "redirect:" + link;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String readAll() {
        AppUser user = currentUserService.getCurrentUser();
        if (user != null) {
            notificationService.markAllSeen(user);
        }
        return "redirect:/notifications";
    }
}
