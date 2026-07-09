package com.rainbow.springboot.thymeleafdemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.Department;
import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;

import jakarta.validation.Valid;

/**
 * Manages Departments (the "one" side of the Department -> Employees relationship).
 * Every method is gated by CAN_VIEW_DEPARTMENT / CAN_MANAGE_DEPARTMENT.
 */
@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService theDepartmentService) {
        departmentService = theDepartmentService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_DEPARTMENT','CAN_MANAGE_DEPARTMENT','SUPER_ADMIN')")
    public String listDepartments(Model theModel) {
        theModel.addAttribute("departments", departmentService.findAll());
        return "departments/list-departments";
    }

    @GetMapping("/showFormForAdd")
    @PreAuthorize("hasAnyAuthority('CAN_MANAGE_DEPARTMENT','SUPER_ADMIN')")
    public String showFormForAdd(Model theModel) {
        theModel.addAttribute("department", new Department());
        return "departments/department-form";
    }

    @GetMapping("/showFormForUpdate")
    @PreAuthorize("hasAnyAuthority('CAN_MANAGE_DEPARTMENT','SUPER_ADMIN')")
    public String showFormForUpdate(@RequestParam("departmentId") int theId, Model theModel) {
        theModel.addAttribute("department", departmentService.findById(theId));
        return "departments/department-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('CAN_MANAGE_DEPARTMENT','SUPER_ADMIN')")
    public String saveDepartment(@Valid @ModelAttribute("department") Department theDepartment,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "departments/department-form";
        }

        departmentService.save(theDepartment);
        redirectAttributes.addFlashAttribute("successMessage",
                "Department \"" + theDepartment.getName() + "\" was saved successfully.");

        return "redirect:/departments/list";
    }

    @GetMapping("/delete")
    @PreAuthorize("hasAnyAuthority('CAN_MANAGE_DEPARTMENT','SUPER_ADMIN')")
    public String delete(@RequestParam("departmentId") int theId, RedirectAttributes redirectAttributes) {
        Department department = departmentService.findById(theId);
        departmentService.deleteById(theId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Department \"" + department.getName() + "\" was deleted.");

        return "redirect:/departments/list";
    }
}
