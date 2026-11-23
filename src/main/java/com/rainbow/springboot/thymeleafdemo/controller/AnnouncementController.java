package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementCategory;
import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementKind;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.AnnouncementService;
import com.rainbow.springboot.thymeleafdemo.service.CommunicationAccess;

/** The noticeboard: Administration and Public announcements, suggestions and issues, replies. */
@Controller
@RequestMapping("/announcements")
@PreAuthorize("isAuthenticated()")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final CurrentUserService currentUserService;
    private final CommunicationAccess access;

    public AnnouncementController(AnnouncementService theAnnouncementService,
                                  CurrentUserService theCurrentUserService,
                                  CommunicationAccess theAccess) {
        announcementService = theAnnouncementService;
        currentUserService = theCurrentUserService;
        access = theAccess;
    }

    @GetMapping
    public String board(@RequestParam(name = "tab", defaultValue = "administration") String tab, Model model) {
        AppUser user = currentUserService.getCurrentUser();
        AnnouncementCategory category = "public".equalsIgnoreCase(tab)
                ? AnnouncementCategory.PUBLIC : AnnouncementCategory.ADMINISTRATION;

        model.addAttribute("tab", category == AnnouncementCategory.PUBLIC ? "public" : "administration");
        model.addAttribute("category", category);
        model.addAttribute("views", announcementService.board(user, category));
        model.addAttribute("manager", access.isManager(user));
        model.addAttribute("adminRestricted", !access.canViewAdministration(user));
        return "announcements/board";
    }

    @PostMapping("/post")
    public String post(@RequestParam("category") AnnouncementCategory category,
                       @RequestParam(name = "kind", defaultValue = "ANNOUNCEMENT") AnnouncementKind kind,
                       @RequestParam("title") String title,
                       @RequestParam("body") String body,
                       RedirectAttributes redirectAttributes) {
        AppUser user = currentUserService.getCurrentUser();
        String tab = category == AnnouncementCategory.PUBLIC ? "public" : "administration";
        try {
            announcementService.post(user, category, kind, title, body);
            if (access.isManager(user)) {
                redirectAttributes.addFlashAttribute("successMessage", "Announcement published and everyone concerned was notified.");
            } else {
                tab = "administration";
                redirectAttributes.addFlashAttribute("successMessage", "Sent to management. They have been notified and can reply here.");
            }
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/announcements?tab=" + tab;
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable("id") int id,
                        @RequestParam("tab") String tab,
                        @RequestParam("body") String body,
                        RedirectAttributes redirectAttributes) {
        try {
            announcementService.reply(currentUserService.getCurrentUser(), id, body);
            redirectAttributes.addFlashAttribute("successMessage", "Reply sent.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/announcements?tab=" + safeTab(tab);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") int id,
                         @RequestParam("tab") String tab,
                         RedirectAttributes redirectAttributes) {
        try {
            announcementService.delete(currentUserService.getCurrentUser(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Post deleted.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/announcements?tab=" + safeTab(tab);
    }

    private static String safeTab(String tab) {
        return "public".equalsIgnoreCase(tab) ? "public" : "administration";
    }
}
