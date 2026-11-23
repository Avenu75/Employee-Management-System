package com.rainbow.springboot.thymeleafdemo.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.MessageService;

/** Private messages between two people. */
@Controller
@RequestMapping("/messages")
@PreAuthorize("isAuthenticated()")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    public MessageController(MessageService theMessageService, CurrentUserService theCurrentUserService) {
        messageService = theMessageService;
        currentUserService = theCurrentUserService;
    }

    @GetMapping
    public String inbox(Model model) {
        AppUser me = currentUserService.getCurrentUser();
        model.addAttribute("conversations", messageService.conversations(me));
        model.addAttribute("recipients", messageService.recipientsFor(me));
        model.addAttribute("canBroadcast", me.isSuperAdmin());
        return "messages/inbox";
    }

    /** Start a new conversation from the inbox. */
    @PostMapping("/new")
    public String start(@RequestParam("recipientId") int recipientId,
                        @RequestParam("body") String body,
                        RedirectAttributes redirectAttributes) {
        AppUser me = currentUserService.getCurrentUser();
        try {
            messageService.send(me, recipientId, body);
            return "redirect:/messages/with/" + recipientId;
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/messages";
        }
    }

    /** Super Admin: one message to one or more people (each gets it as a private message). */
    @PostMapping("/broadcast")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public String broadcast(@RequestParam(name = "recipientIds", required = false) List<Integer> recipientIds,
                            @RequestParam("body") String body,
                            RedirectAttributes redirectAttributes) {
        try {
            int sent = messageService.broadcast(currentUserService.getCurrentUser(), recipientIds, body);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Message sent to " + sent + (sent == 1 ? " person." : " people."));
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/messages";
    }

    @GetMapping("/with/{userId}")
    public String thread(@PathVariable("userId") int userId, Model model, RedirectAttributes redirectAttributes) {
        AppUser me = currentUserService.getCurrentUser();
        if (userId == me.getId()) {
            return "redirect:/messages";
        }
        AppUser other;
        try {
            other = messageService.findUser(userId);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "That person could not be found.");
            return "redirect:/messages";
        }
        model.addAttribute("other", other);
        model.addAttribute("messages", messageService.thread(me, other));
        model.addAttribute("canReply", messageService.canMessage(me, userId));
        model.addAttribute("maxLength", MessageService.MAX_LENGTH);
        return "messages/thread";
    }

    @PostMapping("/with/{userId}/send")
    public String send(@PathVariable("userId") int userId,
                       @RequestParam("body") String body,
                       RedirectAttributes redirectAttributes) {
        try {
            messageService.send(currentUserService.getCurrentUser(), userId, body);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/messages/with/" + userId;
    }
}
