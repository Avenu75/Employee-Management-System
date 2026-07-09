package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;

/**
 * Serves the STAFF dashboard at /dashboard - a small "at a glance" view with
 * headline numbers and a per-department breakdown, for any logged-in user
 * who has at least one management-style permission (view/add/edit/delete
 * employee, department, reports, or attendance).
 *
 * NOTE: unlike the earlier version of this project, "/" is now the PUBLIC
 * landing page (see PublicController) - nobody lands here without logging
 * in first, and plain employees with no granted permissions are instead
 * redirected to /profile by PrivilegeBasedLoginSuccessHandler.
 */
@Controller
public class DashboardController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public DashboardController(EmployeeService theEmployeeService, DepartmentService theDepartmentService) {
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE'," +
            "'CAN_DELETE_EMPLOYEE','CAN_VIEW_DEPARTMENT','CAN_MANAGE_DEPARTMENT'," +
            "'CAN_VIEW_REPORTS','CAN_MANAGE_ATTENDANCE','SUPER_ADMIN')")
    public String dashboard(Model theModel) {

        theModel.addAttribute("totalEmployees", employeeService.countTotal());
        theModel.addAttribute("activeEmployees", employeeService.countActive());
        theModel.addAttribute("inactiveEmployees", employeeService.countInactive());
        theModel.addAttribute("totalDepartments", departmentService.findAll().size());
        theModel.addAttribute("departmentCounts", employeeService.countByDepartment());

        return "dashboard";
    }
}
