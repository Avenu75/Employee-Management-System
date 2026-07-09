package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;

/**
 * A small reporting view, gated by CAN_VIEW_REPORTS - gives whoever is
 * granted that single permission visibility into headcount and department
 * breakdowns WITHOUT also granting them the ability to add/edit/delete
 * employees. This is the clearest illustration in the whole application of
 * why action-based permissions are more flexible than fixed roles: "can see
 * reports" and "can edit employees" are now two completely independent
 * grants.
 */
@Controller
public class ReportsController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public ReportsController(EmployeeService theEmployeeService, DepartmentService theDepartmentService) {
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_REPORTS','SUPER_ADMIN')")
    public String reports(Model model) {
        model.addAttribute("totalEmployees", employeeService.countTotal());
        model.addAttribute("activeEmployees", employeeService.countActive());
        model.addAttribute("inactiveEmployees", employeeService.countInactive());
        model.addAttribute("departmentCounts", employeeService.countByDepartment());
        model.addAttribute("departments", departmentService.findAll());
        return "reports";
    }
}
