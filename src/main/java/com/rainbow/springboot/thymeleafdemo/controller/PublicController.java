package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles the small set of pages reachable WITHOUT logging in:
 *  - "/"       the public marketing/landing page (hero section, no data)
 *  - "/login"  the actual sign-in form
 *  - "/access-denied"  shown when an authenticated user's @PreAuthorize check fails
 *
 * No employee, department, or other business data is ever loaded or
 * referenced by any method in this controller - that is the whole point of
 * the "public landing page" requirement: nothing sensitive is visible
 * before authentication.
 */
@Controller
public class PublicController {

    @GetMapping("/")
    public String landingPage() {
        return "public/landing";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "public/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "public/access-denied";
    }
}
