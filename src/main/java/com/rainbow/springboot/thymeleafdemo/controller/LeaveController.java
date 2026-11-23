package com.rainbow.springboot.thymeleafdemo.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveType;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.LeaveService;

/**
 * Leave pages.
 *   /leave          - any signed-in user with an employee record: balance, request form, own history
 *   /leave/manage   - CAN_MANAGE_LEAVE (or super admin): approve or reject requests
 */
@Controller
@RequestMapping("/leave")
public class LeaveController {

    private final LeaveService leaveService;
    private final CurrentUserService currentUserService;

    public LeaveController(LeaveService theLeaveService, CurrentUserService theCurrentUserService) {
        leaveService = theLeaveService;
        currentUserService = theCurrentUserService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String myLeave(Model model) {
        Employee employee = currentEmployee();
        model.addAttribute("linked", employee != null);
        if (employee != null) {
            model.addAttribute("balance", leaveService.balance(employee, LocalDate.now().getYear()));
            model.addAttribute("history", leaveService.history(employee));
            model.addAttribute("types", LeaveType.values());
            model.addAttribute("year", LocalDate.now().getYear());
            model.addAttribute("today", LocalDate.now());
        }
        return "leave/my-leave";
    }

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public String submit(@RequestParam("leaveType") LeaveType type,
                         @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         @RequestParam(value = "reason", required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            leaveService.request(currentEmployee(), type, startDate, endDate, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Leave request submitted. A manager will review it.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/leave";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public String cancel(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            leaveService.cancel(id, currentEmployee());
            redirectAttributes.addFlashAttribute("successMessage", "Leave request cancelled.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/leave";
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAnyAuthority('CAN_MANAGE_LEAVE','SUPER_ADMIN')")
    public String manage(Model model) {
        model.addAttribute("pending", leaveService.pending());
        model.addAttribute("decided", leaveService.recentlyDecided());
        return "leave/manage-leave";
    }

    @PostMapping("/{id}/decide")
    @PreAuthorize("hasAnyAuthority('CAN_MANAGE_LEAVE','SUPER_ADMIN')")
    public String decide(@PathVariable int id,
                         @RequestParam("decision") String decision,
                         @RequestParam(value = "comment", required = false) String comment,
                         RedirectAttributes redirectAttributes) {
        try {
            boolean approve = "approve".equalsIgnoreCase(decision);
            leaveService.decide(id, approve, comment, currentUserService.getCurrentUser());
            redirectAttributes.addFlashAttribute("successMessage", approve ? "Request approved." : "Request rejected.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/leave/manage";
    }

    private Employee currentEmployee() {
        AppUser user = currentUserService.getCurrentUser();
        return user != null ? user.getEmployee() : null;
    }
}
