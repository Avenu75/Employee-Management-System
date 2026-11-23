package com.rainbow.springboot.thymeleafdemo.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeDocument;
import com.rainbow.springboot.thymeleafdemo.entity.Gender;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeDocumentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;
import com.rainbow.springboot.thymeleafdemo.service.SensitiveChangeService;
import com.rainbow.springboot.thymeleafdemo.service.UserManagementService;

/**
 * Self-service profile view. This is where a plain employee - someone who
 * has been given a login account but has not been granted ANY management
 * permission - lands after logging in (see
 * PrivilegeBasedLoginSuccessHandler). They can see their own basic details
 * and, if linked, their own HR record, but nothing about other employees.
 */
@Controller
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final UserManagementService userManagementService;
    private final EmployeeService employeeService;
    private final EmployeeDocumentService employeeDocumentService;
    private final AppUserRepository appUserRepository;
    private final SensitiveChangeService sensitiveChangeService;

    public ProfileController(CurrentUserService theCurrentUserService,
                             UserManagementService theUserManagementService,
                             EmployeeService theEmployeeService,
                             EmployeeDocumentService theEmployeeDocumentService,
                             AppUserRepository theAppUserRepository,
                             SensitiveChangeService theSensitiveChangeService) {
        sensitiveChangeService = theSensitiveChangeService;
        currentUserService = theCurrentUserService;
        userManagementService = theUserManagementService;
        employeeService = theEmployeeService;
        employeeDocumentService = theEmployeeDocumentService;
        appUserRepository = theAppUserRepository;
    }

    // Any signed-in user may see their OWN profile (it only ever shows their own data).
    // Editing still needs CAN_EDIT_OWN_PROFILE.
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String viewProfile(Model model) {
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Employee employee = currentUser.getEmployee();
        if (employee != null) {
            if (employee.getDepartment() != null) {
                employee.getDepartment().getName();
            }
            if (employee.getStatus() != null) {
                employee.getStatus().name();
            }
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("employee", employee);
        model.addAttribute("documents", employee != null ? employeeDocumentService.findByEmployee(employee) : java.util.List.of());
        return "profile";
    }

    @GetMapping("/profile/edit")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_OWN_PROFILE','SUPER_ADMIN')")
    public String showEditProfile(Model model) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Employee employee = currentUser != null ? currentUser.getEmployee() : null;
        if (employee == null) {
            employee = new Employee();
            employee.setEmail(currentUser != null ? currentUser.getEmail() : null);
            if (currentUser != null && currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
                String[] names = currentUser.getFullName().trim().split("\\s+", 2);
                employee.setFirstName(names.length > 0 ? names[0] : "");
                employee.setLastName(names.length > 1 ? names[1] : "");
            }
        }
        model.addAttribute("employee", employee);
        model.addAttribute("genders", Gender.values());
        return "profile-edit";
    }

    @PostMapping("/profile/update")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_OWN_PROFILE','SUPER_ADMIN')")
    public String updateProfile(@ModelAttribute("employee") Employee employee,
                                @RequestParam(value = "profilePhoto", required = false) MultipartFile profileImage,
                                RedirectAttributes redirectAttributes) {
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Employee managedEmployee = currentUser.getEmployee();
        if (managedEmployee == null) {
            managedEmployee = new Employee();
            // Required columns: HR can correct these later; they are never taken from the form.
            managedEmployee.setSalary(java.math.BigDecimal.ZERO);
            managedEmployee.setHireDate(java.time.LocalDate.now());
            managedEmployee.setStatus(com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus.ACTIVE);
            currentUser.setEmployee(managedEmployee);
        }

        // Remember the sensitive fields as they are now (null for a brand-new record).
        Map<String, String> before = managedEmployee.getId() != 0 ? sensitiveChangeService.snapshot(managedEmployee) : null;

        // SECURITY: a user editing their OWN profile may only change personal
        // details. Salary, hire date, department, status and job title are HR-controlled
        // and are deliberately never copied from the submitted form.
        managedEmployee.setFirstName(employee.getFirstName());
        managedEmployee.setLastName(employee.getLastName());
        managedEmployee.setEmail(employee.getEmail());
        managedEmployee.setPhoneNumber(employee.getPhoneNumber());
        managedEmployee.setNationalId(employee.getNationalId());
        managedEmployee.setDateOfBirth(employee.getDateOfBirth());
        managedEmployee.setGender(employee.getGender());

        if (profileImage != null && !profileImage.isEmpty()) {
            String contentType = profileImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please upload a valid image file.");
                return "redirect:/profile/edit";
            }
            try {
                String encoded = Base64.getEncoder().encodeToString(profileImage.getBytes());
                managedEmployee.setProfileImage("data:" + contentType + ";base64," + encoded);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "The image could not be processed. Please try again.");
                return "redirect:/profile/edit";
            }
        }

        employeeService.save(managedEmployee);
        currentUser.setFullName(managedEmployee.getFullName());
        currentUser.setEmail(managedEmployee.getEmail());
        appUserRepository.save(currentUser);

        if (before != null) {
            sensitiveChangeService.reportEmployeeChange(currentUser, managedEmployee, before);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Your profile was updated successfully.");
        return "redirect:/profile";
    }

    /** A signed-in employee adds documents to their own record. */
    @PostMapping("/profile/documents/upload")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_OWN_PROFILE','SUPER_ADMIN')")
    public String uploadOwnDocuments(@RequestParam(value = "files", required = false) MultipartFile[] files,
                                     RedirectAttributes redirectAttributes) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Employee employee = currentUser != null ? currentUser.getEmployee() : null;
        if (employee == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your account is not linked to an employee record, so documents cannot be stored.");
            return "redirect:/profile";
        }

        List<String> saved = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
                try {
                    employeeDocumentService.saveDocument(employee, file, "Uploaded by the employee");
                    saved.add(name);
                } catch (RuntimeException ex) {
                    problems.add(name + ": " + ex.getMessage());
                }
            }
        }

        if (!saved.isEmpty()) {
            sensitiveChangeService.reportDocumentChange(currentUser, employee, "uploaded", saved);
            redirectAttributes.addFlashAttribute("successMessage",
                    saved.size() + " document(s) uploaded: " + String.join(", ", saved) + ".");
        }
        if (!problems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", String.join(" ", problems));
        }
        if (saved.isEmpty() && problems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose at least one file to upload.");
        }
        return "redirect:/profile";
    }

    @GetMapping("/profile/change-password")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_OWN_PROFILE','SUPER_ADMIN')")
    public String showChangePasswordPage(Model model) {
        model.addAttribute("user", currentUserService.getCurrentUser());
        return "profile-change-password";
    }

    @PostMapping("/profile/change-password")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_OWN_PROFILE','SUPER_ADMIN')")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/profile/change-password";
        }

        try {
            userManagementService.changePassword(currentUser, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Your password was updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile/change-password";
        }

        return "redirect:/profile";
    }
}
