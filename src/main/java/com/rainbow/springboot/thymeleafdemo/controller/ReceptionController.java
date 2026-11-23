package com.rainbow.springboot.thymeleafdemo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.dto.GateRow;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeGateService;
import com.rainbow.springboot.thymeleafdemo.service.WorkerAttendanceService;

/** The reception gate: take attendance for employees and other workers as they arrive and leave. */
@Controller
@RequestMapping("/reception")
@PreAuthorize("hasAuthority('CAN_USE_RECEPTION')")
public class ReceptionController {

    private static final String WORKERS = "workers";
    private static final String EMPLOYEES = "employees";

    private final WorkerAttendanceService workerAttendanceService;
    private final EmployeeGateService employeeGateService;
    private final CurrentUserService currentUserService;

    public ReceptionController(WorkerAttendanceService theWorkerAttendanceService,
                               EmployeeGateService theEmployeeGateService,
                               CurrentUserService theCurrentUserService) {
        workerAttendanceService = theWorkerAttendanceService;
        employeeGateService = theEmployeeGateService;
        currentUserService = theCurrentUserService;
    }

    @GetMapping
    public String board(@RequestParam(name = "group", defaultValue = EMPLOYEES) String groupText,
                        @RequestParam(name = "date", required = false) String dateText,
                        @RequestParam(name = "q", required = false) String q,
                        Model model) {
        String group = group(groupText);
        LocalDate today = LocalDate.now();
        LocalDate date = today;
        if (dateText != null && !dateText.isBlank()) {
            try {
                date = LocalDate.parse(dateText);
            } catch (DateTimeParseException ex) {
                date = today;
            }
        }
        if (date.isAfter(today)) {
            date = today;
        }

        List<GateRow> everyone = WORKERS.equals(group) ? workerAttendanceService.board(date) : employeeGateService.board(date);
        model.addAttribute("group", group);
        model.addAttribute("date", date);
        model.addAttribute("today", today);
        model.addAttribute("isToday", date.equals(today));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("rows", GateRow.filter(everyone, q));
        model.addAttribute("summary", GateRow.summary(everyone));
        return "reception/reception";
    }

    @PostMapping("/check-in")
    public String checkIn(@RequestParam("group") String group, @RequestParam("id") int id,
                          @RequestParam(name = "q", required = false) String q, RedirectAttributes ra) {
        return act("in", group, id, q, ra);
    }

    @PostMapping("/check-out")
    public String checkOut(@RequestParam("group") String group, @RequestParam("id") int id,
                           @RequestParam(name = "q", required = false) String q, RedirectAttributes ra) {
        return act("out", group, id, q, ra);
    }

    @PostMapping("/absent")
    public String absent(@RequestParam("group") String group, @RequestParam("id") int id,
                         @RequestParam(name = "q", required = false) String q, RedirectAttributes ra) {
        return act("absent", group, id, q, ra);
    }

    @PostMapping("/clear")
    public String clear(@RequestParam("group") String group, @RequestParam("id") int id,
                        @RequestParam(name = "q", required = false) String q, RedirectAttributes ra) {
        return act("clear", group, id, q, ra);
    }

    private String act(String what, String groupText, int id, String q, RedirectAttributes ra) {
        String group = group(groupText);
        boolean workers = WORKERS.equals(group);
        AppUser user = currentUserService.getCurrentUser();
        try {
            switch (what) {
                case "in":
                    if (workers) { workerAttendanceService.checkIn(id, user); } else { employeeGateService.checkIn(id); }
                    ra.addFlashAttribute("successMessage", "Checked in.");
                    break;
                case "out":
                    if (workers) { workerAttendanceService.checkOut(id, user); } else { employeeGateService.checkOut(id); }
                    ra.addFlashAttribute("successMessage", "Checked out.");
                    break;
                case "absent":
                    if (workers) { workerAttendanceService.markAbsent(id, user); } else { employeeGateService.markAbsent(id); }
                    ra.addFlashAttribute("successMessage", "Marked absent.");
                    break;
                default:
                    if (workers) { workerAttendanceService.clearToday(id); } else { employeeGateService.clearToday(id); }
                    ra.addFlashAttribute("successMessage", "Entry cleared.");
                    break;
            }
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        ra.addAttribute("group", group);
        if (q != null && !q.isBlank()) {
            ra.addAttribute("q", q);
        }
        return "redirect:/reception";
    }

    private static String group(String text) {
        return WORKERS.equalsIgnoreCase(text) ? WORKERS : EMPLOYEES;
    }
}
