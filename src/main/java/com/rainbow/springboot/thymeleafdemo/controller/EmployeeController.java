package com.rainbow.springboot.thymeleafdemo.controller;

import java.io.PrintWriter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Handles all HTTP requests related to Employees:
 *  - list / search / pagination
 *  - add / update (same form, reused)
 *  - delete
 *  - CSV export
 *
 * The Controller is intentionally "thin": it only deals with HTTP concerns
 * (reading request parameters, choosing a view to render) and delegates all
 * real work to EmployeeService.
 *
 * ACCESS CONTROL: every method below is gated with @PreAuthorize against one
 * of the dynamic Permission codes (CAN_VIEW_EMPLOYEE, CAN_ADD_EMPLOYEE, ...).
 * The Super Admin authority also satisfies every check, so the Super Admin
 * can always operate the system even before granting themself any specific
 * business permission (they are not meant to need one - system
 * administration is their job, not day-to-day HR data entry, but this keeps
 * the account fully functional regardless).
 */
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService theEmployeeService, DepartmentService theDepartmentService) {
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','SUPER_ADMIN')")
    public String listEmployees(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sortBy", defaultValue = "lastName") String sortBy,
            Model theModel) {

        Page<Employee> employeePage = employeeService.search(keyword, page, size, sortBy);

        theModel.addAttribute("employeePage", employeePage);
        theModel.addAttribute("employees", employeePage.getContent());
        theModel.addAttribute("keyword", keyword);
        theModel.addAttribute("sortBy", sortBy);
        theModel.addAttribute("currentPage", page);
        theModel.addAttribute("pageSize", size);

        return "employees/list-employees";
    }

    @GetMapping("/showFormForAdd")
    @PreAuthorize("hasAnyAuthority('CAN_ADD_EMPLOYEE','SUPER_ADMIN')")
    public String showFormForAdd(Model theModel) {

        Employee theEmployee = new Employee();

        theModel.addAttribute("employee", theEmployee);
        theModel.addAttribute("departments", departmentService.findAll());

        return "employees/employee-form";
    }

    @GetMapping("/showFormForUpdate")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String showFormForUpdate(@RequestParam("employeeId") int theId, Model theModel) {

        Employee theEmployee = employeeService.findById(theId);

        theModel.addAttribute("employee", theEmployee);
        theModel.addAttribute("departments", departmentService.findAll());

        return "employees/employee-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String saveEmployee(@Valid @ModelAttribute("employee") Employee theEmployee,
                                org.springframework.validation.BindingResult bindingResult,
                                Model theModel,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // Validation failed (e.g. blank name, invalid email) - redisplay the
            // form together with the error messages instead of saving bad data.
            theModel.addAttribute("departments", departmentService.findAll());
            return "employees/employee-form";
        }

        boolean isNewEmployee = (theEmployee.getId() == 0);
        employeeService.save(theEmployee);

        redirectAttributes.addFlashAttribute("successMessage",
                isNewEmployee
                        ? "Employee \"" + theEmployee.getFullName() + "\" was added successfully."
                        : "Employee \"" + theEmployee.getFullName() + "\" was updated successfully.");

        // Redirect (rather than forward) to prevent duplicate form submissions on refresh
        return "redirect:/employees/list";
    }

    @GetMapping("/delete")
    @PreAuthorize("hasAnyAuthority('CAN_DELETE_EMPLOYEE','SUPER_ADMIN')")
    public String delete(@RequestParam("employeeId") int theId, RedirectAttributes redirectAttributes) {

        Employee theEmployee = employeeService.findById(theId);
        employeeService.deleteById(theId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Employee \"" + theEmployee.getFullName() + "\" was deleted.");

        return "redirect:/employees/list";
    }

    /**
     * Exports every employee to a downloadable CSV file - a common
     * requirement for small businesses (e.g. for payroll or reporting in Excel).
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_REPORTS','CAN_VIEW_EMPLOYEE','SUPER_ADMIN')")
    public void exportToCsv(HttpServletResponse response) throws Exception {

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"");

        List<Employee> employees = employeeService.findAll();

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,First Name,Last Name,Email,Phone,Job Title,Salary,Hire Date,Status,Department");
            for (Employee e : employees) {
                String departmentName = e.getDepartment() != null ? e.getDepartment().getName() : "";
                writer.println(String.join(",",
                        String.valueOf(e.getId()),
                        csvSafe(e.getFirstName()),
                        csvSafe(e.getLastName()),
                        csvSafe(e.getEmail()),
                        csvSafe(e.getPhoneNumber()),
                        csvSafe(e.getJobTitle()),
                        String.valueOf(e.getSalary()),
                        String.valueOf(e.getHireDate()),
                        String.valueOf(e.getStatus()),
                        csvSafe(departmentName)));
            }
        }
    }

    private String csvSafe(String value) {
        if (value == null) {
            return "";
        }
        return value.contains(",") ? "\"" + value + "\"" : value;
    }
}
