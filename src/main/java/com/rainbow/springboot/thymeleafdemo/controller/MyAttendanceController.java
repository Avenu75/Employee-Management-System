package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.AttendanceService;

/** Clock in and clock out for the signed-in employee (no special permission needed). */
@Controller
@RequestMapping("/my-attendance")
@PreAuthorize("isAuthenticated()")
public class MyAttendanceController {

    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;

    public MyAttendanceController(AttendanceService theAttendanceService, CurrentUserService theCurrentUserService) {
        attendanceService = theAttendanceService;
        currentUserService = theCurrentUserService;
    }

    @GetMapping
    public String view(Model model) {
        Employee employee = currentEmployee();
        model.addAttribute("linked", employee != null);
        if (employee != null) {
            model.addAttribute("today", attendanceService.findToday(employee.getId()));
            model.addAttribute("records", attendanceService.recent(employee.getId()));
        }
        return "my-attendance";
    }

    @PostMapping("/clock-in")
    public String clockIn(RedirectAttributes redirectAttributes) {
        return act(true, redirectAttributes);
    }

    @PostMapping("/clock-out")
    public String clockOut(RedirectAttributes redirectAttributes) {
        return act(false, redirectAttributes);
    }

    private String act(boolean in, RedirectAttributes redirectAttributes) {
        Employee employee = currentEmployee();
        if (employee == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your account is not linked to an employee record.");
            return "redirect:/my-attendance";
        }
        try {
            if (in) {
                attendanceService.clockIn(employee.getId());
                redirectAttributes.addFlashAttribute("successMessage", "You are clocked in.");
            } else {
                attendanceService.clockOut(employee.getId());
                redirectAttributes.addFlashAttribute("successMessage", "You are clocked out. Have a good evening.");
            }
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/my-attendance";
    }

    private Employee currentEmployee() {
        AppUser user = currentUserService.getCurrentUser();
        return user != null ? user.getEmployee() : null;
    }
}
