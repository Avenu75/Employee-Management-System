package com.rainbow.springboot.thymeleafdemo.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.dto.GrantPermissionRequest;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.security.CustomUserDetailsService;
import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;
import com.rainbow.springboot.thymeleafdemo.service.UserManagementService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * System-administration area: only reachable by the single Super Admin
 * account (enforced both at the URL level in SecurityConfig via
 * hasAuthority(SUPER_ADMIN) AND again here with @PreAuthorize on every
 * single method - defense in depth, so this controller remains safe even
 * if the URL-level rule were ever accidentally loosened).
 *
 * This is where the "action-based privilege granting" requirement lives:
 * rather than assigning a fixed role, the Super Admin ticks/unticks
 * individual Permission checkboxes per staff user.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
public class AdminController {

    private final UserManagementService userManagementService;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final CurrentUserService currentUserService;

    public AdminController(UserManagementService theUserManagementService,
                            EmployeeService theEmployeeService,
                            DepartmentService theDepartmentService,
                            CurrentUserService theCurrentUserService) {
        userManagementService = theUserManagementService;
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
        currentUserService = theCurrentUserService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalEmployees", employeeService.countTotal());
        model.addAttribute("activeEmployees", employeeService.countActive());
        model.addAttribute("totalDepartments", departmentService.findAll().size());
        model.addAttribute("totalStaffUsers", userManagementService.findAllStaffUsers().size());
        return "admin/admin-dashboard";
    }

    /**
     * The main "grant/revoke privilege" screen: lists every non-super-admin
     * user next to checkboxes for every known Permission, pre-ticked
     * according to what is currently granted.
     */
    @GetMapping("/privileges")
    public String managePrivileges(Model model) {
        List<AppUser> users = userManagementService.findAllStaffUsers();
        model.addAttribute("users", users);
        model.addAttribute("allPermissions", userManagementService.findAllPermissions());
        return "admin/manage-privileges";
    }

    @PostMapping("/privileges/grant")
    public String grantPrivilege(@ModelAttribute GrantPermissionRequest request,
                                  RedirectAttributes redirectAttributes) {
        AppUser currentAdmin = currentUserService.getCurrentUser();
        userManagementService.grantPermission(request.getUserId(), request.getPermissionId(), currentAdmin);

        AppUser target = userManagementService.findById(request.getUserId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Privilege granted to \"" + target.getFullName() + "\".");

        return "redirect:/admin/privileges";
    }

    @PostMapping("/privileges/revoke")
    public String revokePrivilege(@ModelAttribute GrantPermissionRequest request,
                                   RedirectAttributes redirectAttributes) {
        userManagementService.revokePermission(request.getUserId(), request.getPermissionId());

        AppUser target = userManagementService.findById(request.getUserId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Privilege revoked from \"" + target.getFullName() + "\".");

        return "redirect:/admin/privileges";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userManagementService.findAllStaffUsers());
        return "admin/manage-users";
    }

    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("newUserForm", new NewStaffUserForm());
        model.addAttribute("employees", employeeService.findAll());
        return "admin/create-user";
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("newUserForm") NewStaffUserForm form,
                              org.springframework.validation.BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("employees", employeeService.findAll());
            return "admin/create-user";
        }

        Integer employeeId = (form.getEmployeeId() != null && form.getEmployeeId() > 0) ? form.getEmployeeId() : null;

        try {
            userManagementService.createStaffUser(form.getUsername(), form.getPassword(), form.getFullName(),
                    form.getEmail(), employeeId);
        } catch (RuntimeException ex) {
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/create-user";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Staff account \"" + form.getUsername() + "\" was created. You can now grant privileges to it.");

        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle-enabled")
    public String toggleEnabled(@RequestParam("userId") int userId,
                                 @RequestParam("enabled") boolean enabled,
                                 RedirectAttributes redirectAttributes) {
        userManagementService.setEnabled(userId, enabled);
        redirectAttributes.addFlashAttribute("successMessage",
                enabled ? "Account enabled." : "Account disabled.");
        return "redirect:/admin/users";
    }

    /** Form-backing object for the "create staff login account" screen. */
    public static class NewStaffUserForm {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Full name is required")
        private String fullName;

        private String email;
        private Integer employeeId;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Integer employeeId) {
            this.employeeId = employeeId;
        }
    }
}
