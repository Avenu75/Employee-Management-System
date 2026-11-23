package com.rainbow.springboot.thymeleafdemo.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.service.AttendanceService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;

/**
 * Simple daily attendance register, gated entirely by CAN_MANAGE_ATTENDANCE -
 * gives that permission a real, working feature to control rather than
 * being an unused entry in the permission catalog.
 */
@Controller
@RequestMapping("/attendance")
@PreAuthorize("hasAuthority('CAN_MANAGE_ATTENDANCE')")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;

    public AttendanceController(AttendanceService theAttendanceService, EmployeeService theEmployeeService) {
        attendanceService = theAttendanceService;
        employeeService = theEmployeeService;
    }

    @GetMapping
    public String viewAttendance(@RequestParam(value = "date", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Model model) {
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("records", attendanceService.findByDate(selectedDate));
        model.addAttribute("summary", attendanceService.summaryForDate(selectedDate));
        model.addAttribute("employees", employeeService.findAll());

        return "attendance";
    }

    @PostMapping("/record")
    public String recordAttendance(@RequestParam("employeeId") int employeeId,
                                    @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam("status") String status,
                                    @RequestParam(value = "note", required = false) String note,
                                    RedirectAttributes redirectAttributes) {

        attendanceService.recordAttendance(employeeId, date, status, note);

        redirectAttributes.addFlashAttribute("successMessage", "Attendance recorded.");

        return "redirect:/attendance?date=" + date;
    }
}
