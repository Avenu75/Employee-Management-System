package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;

/**
 * Self-service profile view. This is where a plain employee - someone who
 * has been given a login account but has not been granted ANY management
 * permission - lands after logging in (see
 * PrivilegeBasedLoginSuccessHandler). They can see their own basic details
 * and, if linked, their own HR record, but nothing about other employees.
 */
@Controller
public class ProfileController {

    private final CurrentUserService currentUserService;

    public ProfileController(CurrentUserService theCurrentUserService) {
        currentUserService = theCurrentUserService;
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        AppUser currentUser = currentUserService.getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("employee", currentUser != null ? currentUser.getEmployee() : null);
        return "profile";
    }
}
