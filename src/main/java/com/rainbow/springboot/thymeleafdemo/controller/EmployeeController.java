package com.rainbow.springboot.thymeleafdemo.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeDocument;
import com.rainbow.springboot.thymeleafdemo.exception.EmployeeNotFoundException;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeDocumentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;
import com.rainbow.springboot.thymeleafdemo.service.PdfExportService;
import com.rainbow.springboot.thymeleafdemo.service.SensitiveChangeService;
import com.rainbow.springboot.thymeleafdemo.util.CsvUtil;

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
    private final EmployeeDocumentService employeeDocumentService;
    private final CurrentUserService currentUserService;
    private final PdfExportService pdfExportService;
    private final SensitiveChangeService sensitiveChangeService;

    public EmployeeController(EmployeeService theEmployeeService,
                              DepartmentService theDepartmentService,
                              EmployeeDocumentService theEmployeeDocumentService,
                              CurrentUserService theCurrentUserService,
                              PdfExportService thePdfExportService,
                              SensitiveChangeService theSensitiveChangeService) {
        pdfExportService = thePdfExportService;
        sensitiveChangeService = theSensitiveChangeService;
        currentUserService = theCurrentUserService;
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
        employeeDocumentService = theEmployeeDocumentService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','SUPER_ADMIN')")
    public String listEmployees(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "departmentId", required = false) Integer departmentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "lastName") String sortBy,
            @RequestParam(value = "dir", defaultValue = "asc") String dir,
            Model theModel) {

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String st = (status == null || status.isBlank()) ? null : status.trim();
        boolean descending = "desc".equalsIgnoreCase(dir);

        Page<Employee> employeePage = employeeService.filter(kw, departmentId, st, page, size, sortBy, descending);

        theModel.addAttribute("employeePage", employeePage);
        theModel.addAttribute("employees", employeePage.getContent());
        theModel.addAttribute("departments", departmentService.findAll());
        theModel.addAttribute("keyword", kw);
        theModel.addAttribute("departmentId", departmentId);
        theModel.addAttribute("status", st);
        theModel.addAttribute("sortBy", sortBy);
        theModel.addAttribute("dir", descending ? "desc" : "asc");
        theModel.addAttribute("currentPage", employeePage.getNumber());
        theModel.addAttribute("pageSize", employeePage.getSize());

        return "employees/list-employees";
    }

    @GetMapping("/profile/{employeeId}")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','SUPER_ADMIN')")
    public String viewEmployeeProfile(@PathVariable int employeeId,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.findById(employeeId);
            if (employee.getDepartment() != null) {
                employee.getDepartment().getName();
            }

            List<EmployeeDocument> documents = employeeDocumentService.findByEmployee(employee);
            model.addAttribute("employee", employee);
            model.addAttribute("documents", documents);
            model.addAttribute("documentCount", documents != null ? documents.size() : 0);
            return "employees/employee-profile";
        } catch (EmployeeNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/employees/list";
        }
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
        theModel.addAttribute("documents", employeeDocumentService.findByEmployee(theEmployee));

        return "employees/employee-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String saveEmployee(@Valid @ModelAttribute("employee") Employee theEmployee,
                               org.springframework.validation.BindingResult bindingResult,
                               // CHANGED: renamed from "profileImage" to "profilePhoto"
                               @RequestParam(value = "profilePhoto", required = false) MultipartFile profileImage,
                               @RequestParam(value = "documents", required = false) MultipartFile[] documents,
                               Model theModel,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            theModel.addAttribute("departments", departmentService.findAll());
            return "employees/employee-form";
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            String contentType = profileImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                theModel.addAttribute("departments", departmentService.findAll());
                theModel.addAttribute("errorMessage", "Please upload a valid image file.");
                return "employees/employee-form";
            }
            try {
                String encoded = Base64.getEncoder().encodeToString(profileImage.getBytes());
                theEmployee.setProfileImage("data:" + contentType + ";base64," + encoded);
            } catch (IOException e) {
                theModel.addAttribute("departments", departmentService.findAll());
                theModel.addAttribute("errorMessage", "The image could not be processed. Please try a different file.");
                return "employees/employee-form";
            }
        }

        boolean isNewEmployee = (theEmployee.getId() == 0);

        // CHANGED: the form does not carry the stored photo, so keep the
        // existing one unless a new file was uploaded.
        Map<String, String> before = null;
        if (!isNewEmployee) {
            Employee existing = employeeService.findById(theEmployee.getId());
            // Remember the sensitive fields as they are NOW, so changes can be reported after the save.
            before = sensitiveChangeService.snapshot(existing);
            if (profileImage == null || profileImage.isEmpty()) {
                theEmployee.setProfileImage(existing.getProfileImage());
            }
        }

        employeeService.save(theEmployee);

        if (before != null) {
            sensitiveChangeService.reportEmployeeChange(currentUserService.getCurrentUser(),
                    employeeService.findById(theEmployee.getId()), before);
        }

        if (documents != null) {
            for (MultipartFile document : documents) {
                if (document != null && !document.isEmpty()) {
                    try {
                        employeeDocumentService.saveDocument(theEmployee, document, "Employee uploaded document");
                    } catch (RuntimeException ex) {
                        theModel.addAttribute("departments", departmentService.findAll());
                        theModel.addAttribute("documents", employeeDocumentService.findByEmployee(theEmployee));
                        theModel.addAttribute("errorMessage", ex.getMessage());
                        return "employees/employee-form";
                    }
                }
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                isNewEmployee
                        ? "Employee \"" + theEmployee.getFullName() + "\" was added successfully."
                        : "Employee \"" + theEmployee.getFullName() + "\" was updated successfully.");

        return "redirect:/employees/list";
    }

    /**
     * Uploads documents for an employee on their own, without saving the rest of the form,
     * and returns to the Update page so the new files are visible straight away.
     */
    @PostMapping("/{employeeId}/documents/upload")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String uploadDocuments(@PathVariable int employeeId,
                                  @RequestParam(value = "files", required = false) MultipartFile[] files,
                                  RedirectAttributes redirectAttributes) {
        Employee employee = employeeService.findById(employeeId);
        List<String> saved = new ArrayList<>();
        List<String> problems = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
                try {
                    employeeDocumentService.saveDocument(employee, file, "Uploaded document");
                    saved.add(name);
                } catch (RuntimeException ex) {
                    problems.add(name + ": " + ex.getMessage());
                }
            }
        }

        if (!saved.isEmpty()) {
            sensitiveChangeService.reportDocumentChange(currentUserService.getCurrentUser(), employee, "uploaded", saved);
            redirectAttributes.addFlashAttribute("successMessage",
                    saved.size() + " document(s) uploaded: " + String.join(", ", saved) + ".");
        }
        if (!problems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", String.join(" ", problems));
        }
        if (saved.isEmpty() && problems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose at least one file to upload.");
        }
        return "redirect:/employees/showFormForUpdate?employeeId=" + employeeId;
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','CAN_VIEW_OWN_PROFILE','SUPER_ADMIN')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable int documentId) {
        EmployeeDocument document = employeeDocumentService.findById(documentId);

        // SECURITY: holding CAN_VIEW_OWN_PROFILE must only expose the user's OWN
        // documents. Anyone else needs CAN_VIEW_EMPLOYEE or SUPER_ADMIN.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean privileged = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CAN_VIEW_EMPLOYEE")
                        || a.getAuthority().equals("SUPER_ADMIN"));
        AppUser currentUser = currentUserService.getCurrentUser();
        boolean owner = currentUser != null && currentUser.getEmployee() != null
                && document.getEmployee() != null
                && currentUser.getEmployee().getId() == document.getEmployee().getId();
        if (!privileged && !owner) {
            throw new AccessDeniedException("Not allowed to download this document.");
        }

        byte[] data = employeeDocumentService.getDocumentBytes(document);
        String safeName = document.getFileName() == null ? "document"
                : document.getFileName().replaceAll("[\\r\\n\"\\\\]", "_");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .body(data);
    }

    @PostMapping("/documents/{documentId}/delete")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_EMPLOYEE','CAN_DELETE_EMPLOYEE','SUPER_ADMIN')")
    public String deleteDocument(@PathVariable int documentId, RedirectAttributes redirectAttributes) {
        EmployeeDocument document = employeeDocumentService.findById(documentId);
        employeeDocumentService.deleteById(documentId);
        sensitiveChangeService.reportDocumentChange(currentUserService.getCurrentUser(), document.getEmployee(),
                "removed", List.of(document.getFileName()));
        redirectAttributes.addFlashAttribute("successMessage",
                "Document \"" + document.getFileName() + "\" was removed.");
        return "redirect:/employees/showFormForUpdate?employeeId=" + document.getEmployee().getId();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAnyAuthority('CAN_DELETE_EMPLOYEE','SUPER_ADMIN')")
    public String delete(@RequestParam("employeeId") int theId, RedirectAttributes redirectAttributes) {

        Employee theEmployee = employeeService.findById(theId);
        employeeService.deleteById(theId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Employee \"" + theEmployee.getFullName() + "\" was deleted.");

        return "redirect:/employees/list";
    }

    /**
     * Downloads the employee list as CSV (opens in Excel). Honours the same
     * keyword / department / status filters as the list page.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_REPORTS','CAN_VIEW_EMPLOYEE','SUPER_ADMIN')")
    public void exportToCsv(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "departmentId", required = false) Integer departmentId,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "sortBy", defaultValue = "lastName") String sortBy,
                            @RequestParam(value = "dir", defaultValue = "asc") String dir,
                            HttpServletResponse response) throws Exception {

        List<Employee> employees = employeeService.filterAll(
                blankToNull(keyword), departmentId, blankToNull(status), sortBy, "desc".equalsIgnoreCase(dir));

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"");

        try (PrintWriter writer = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // byte-order mark so Excel reads accents correctly
            writer.println("ID,First Name,Last Name,Email,Phone,Job Title,Salary,Hire Date,Status,Department");
            for (Employee e : employees) {
                String departmentName = e.getDepartment() != null ? e.getDepartment().getName() : "";
                writer.println(CsvUtil.row(
                        String.valueOf(e.getId()), e.getFirstName(), e.getLastName(), e.getEmail(),
                        e.getPhoneNumber(), e.getJobTitle(), String.valueOf(e.getSalary()),
                        String.valueOf(e.getHireDate()), String.valueOf(e.getStatus()), departmentName));
            }
        }
    }

    /** Downloads the same filtered list as a PDF. */
    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_REPORTS','CAN_VIEW_EMPLOYEE','SUPER_ADMIN')")
    public void exportToPdf(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "departmentId", required = false) Integer departmentId,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "sortBy", defaultValue = "lastName") String sortBy,
                            @RequestParam(value = "dir", defaultValue = "asc") String dir,
                            HttpServletResponse response) throws Exception {

        String kw = blankToNull(keyword);
        String st = blankToNull(status);
        List<Employee> employees = employeeService.filterAll(kw, departmentId, st, sortBy, "desc".equalsIgnoreCase(dir));

        StringBuilder description = new StringBuilder();
        if (kw != null) {
            description.append("search \"").append(kw).append("\" ");
        }
        if (departmentId != null) {
            departmentService.findAll().stream().filter(d -> d.getId() == departmentId).findFirst()
                    .ifPresent(d -> description.append("department ").append(d.getName()).append(' '));
        }
        if (st != null) {
            description.append("status ").append(st).append(' ');
        }

        byte[] pdf = pdfExportService.employeesPdf(employees, description.toString().trim());
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
