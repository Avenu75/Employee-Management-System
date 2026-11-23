package com.rainbow.springboot.thymeleafdemo.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.dto.GrantPermissionRequest;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Permission;
import com.rainbow.springboot.thymeleafdemo.entity.SecurityAuditLog;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.security.CustomUserDetailsService;
import com.rainbow.springboot.thymeleafdemo.security.PermissionCode;
import com.rainbow.springboot.thymeleafdemo.security.RolePreset;
import com.rainbow.springboot.thymeleafdemo.service.AuditLogService;
import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;
import com.rainbow.springboot.thymeleafdemo.service.SensitiveChangeService;
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
public class AdminController {

    private final UserManagementService userManagementService;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final SensitiveChangeService sensitiveChangeService;

    public AdminController(UserManagementService theUserManagementService,
                           EmployeeService theEmployeeService,
                           DepartmentService theDepartmentService,
                           CurrentUserService theCurrentUserService,
                           AuditLogService theAuditLogService,
                           SensitiveChangeService theSensitiveChangeService) {
        sensitiveChangeService = theSensitiveChangeService;
        userManagementService = theUserManagementService;
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
        currentUserService = theCurrentUserService;
        auditLogService = theAuditLogService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String adminDashboard(Model model) {
        model.addAttribute("totalEmployees", employeeService.countTotal());
        model.addAttribute("activeEmployees", employeeService.countActive());
        model.addAttribute("inactiveEmployees", employeeService.countInactive());
        model.addAttribute("totalDepartments", departmentService.findAll().size());

        List<AppUser> staff = userManagementService.findAllStaffUsers();
        model.addAttribute("totalStaffUsers", staff.size());
        model.addAttribute("disabledStaffUsers", staff.stream().filter(u -> !u.isEnabled()).count());
        model.addAttribute("usersWithoutPrivileges",
                staff.stream().filter(u -> u.getGrantedPermissions().isEmpty()).count());

        List<SecurityAuditLog> recent = auditLogService.findRecentEntries();
        model.addAttribute("recentAudit", recent.size() > 6 ? recent.subList(0, 6) : recent);
        return "admin/admin-dashboard";
    }

    /**
     * The main "grant/revoke privilege" screen: lists every non-super-admin
     * user next to checkboxes for every known Permission, pre-ticked
     * according to what is currently granted.
     */
    @GetMapping("/privileges")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String managePrivileges(Model model) {
        List<AppUser> users = userManagementService.findAllStaffUsers();
        model.addAttribute("users", users);
        model.addAttribute("allPermissions", userManagementService.findAllPermissions());
        return "admin/manage-privileges";
    }

    /**
     * The privilege editor for ONE person: every privilege grouped by category with a tick box,
     * quick-fill buttons, and a clear Save button.
     */
    @GetMapping("/privileges/{userId}")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String editPrivileges(@PathVariable int userId, Model model) {
        AppUser target = userManagementService.findById(userId);

        Map<String, List<Permission>> grouped = new LinkedHashMap<>();
        for (Permission permission : userManagementService.findAllPermissions()) {
            String category = permission.getCategory() == null ? "General" : permission.getCategory();
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(permission);
        }

        Set<Integer> grantedIds = userManagementService.getGrantedPermissions(target).stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        model.addAttribute("target", target);
        model.addAttribute("groups", grouped);
        model.addAttribute("grantedIds", grantedIds);
        model.addAttribute("staffCodes", new ArrayList<>(RolePreset.STAFF.getPermissions()));
        model.addAttribute("adminCodes", new ArrayList<>(RolePreset.ADMIN.getPermissions()));
        return "admin/edit-privileges";
    }

    @PostMapping("/privileges/grant")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String grantPrivilege(@ModelAttribute GrantPermissionRequest request,
                                 RedirectAttributes redirectAttributes) {
        AppUser currentAdmin = currentUserService.getCurrentUser();
        userManagementService.grantPermission(request.getUserId(), request.getPermissionId(), currentAdmin);

        AppUser target = userManagementService.findById(request.getUserId());
        userManagementService.findAllPermissions().stream()
                .filter(p -> p.getId() == request.getPermissionId()).findFirst()
                .ifPresent(p -> sensitiveChangeService.reportPrivilegeChange(currentAdmin, target,
                        List.of(p.getDisplayName()), List.of(), SensitiveChangeService.isBigPrivilege(p.getCode())));
        redirectAttributes.addFlashAttribute("successMessage",
                "Privilege granted to \"" + target.getFullName() + "\".");

        return "redirect:/admin/privileges";
    }

    @PostMapping("/privileges/revoke")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String revokePrivilege(@ModelAttribute GrantPermissionRequest request,
                                  RedirectAttributes redirectAttributes) {
        userManagementService.revokePermission(request.getUserId(), request.getPermissionId());

        AppUser target = userManagementService.findById(request.getUserId());
        AppUser currentAdmin = currentUserService.getCurrentUser();
        userManagementService.findAllPermissions().stream()
                .filter(p -> p.getId() == request.getPermissionId()).findFirst()
                .ifPresent(p -> sensitiveChangeService.reportPrivilegeChange(currentAdmin, target,
                        List.of(), List.of(p.getDisplayName()), false));
        redirectAttributes.addFlashAttribute("successMessage",
                "Privilege revoked from \"" + target.getFullName() + "\".");

        return "redirect:/admin/privileges";
    }

    /**
     * Saves the full set of privileges ticked in the "Edit privileges" dialog:
     * newly ticked boxes are granted, newly un-ticked boxes are revoked.
     */
    @PostMapping("/privileges/save")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String savePrivileges(@RequestParam("userId") int userId,
                                 @RequestParam(value = "permissionIds", required = false) List<Integer> permissionIds,
                                 RedirectAttributes redirectAttributes) {
        AppUser admin = currentUserService.getCurrentUser();
        AppUser target = userManagementService.findById(userId);

        Set<Integer> current = userManagementService.getGrantedPermissions(target).stream()
                .map(permission -> permission.getId())
                .collect(Collectors.toSet());
        Set<Integer> wanted = permissionIds == null ? new HashSet<>() : new HashSet<>(permissionIds);

        Map<Integer, Permission> byId = new LinkedHashMap<>();
        for (Permission permission : userManagementService.findAllPermissions()) {
            byId.put(permission.getId(), permission);
        }

        int granted = 0;
        int revoked = 0;
        List<String> grantedNames = new ArrayList<>();
        List<String> revokedNames = new ArrayList<>();
        boolean elevated = false;
        try {
            for (Integer id : wanted) {
                if (!current.contains(id) && byId.containsKey(id)) {
                    userManagementService.grantPermission(userId, id, admin);
                    granted++;
                    grantedNames.add(byId.get(id).getDisplayName());
                    elevated = elevated || SensitiveChangeService.isBigPrivilege(byId.get(id).getCode());
                }
            }
            for (Integer id : current) {
                if (!wanted.contains(id)) {
                    userManagementService.revokePermission(userId, id);
                    revoked++;
                    revokedNames.add(byId.containsKey(id) ? byId.get(id).getDisplayName() : "Privilege " + id);
                }
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/privileges/" + userId;
        }

        sensitiveChangeService.reportPrivilegeChange(admin, target, grantedNames, revokedNames, elevated);

        redirectAttributes.addFlashAttribute("successMessage",
                (granted == 0 && revoked == 0)
                        ? "No changes were made for \"" + target.getFullName() + "\"."
                        : "Privileges updated for \"" + target.getFullName() + "\": "
                          + granted + " granted, " + revoked + " revoked.");
        return "redirect:/admin/privileges";
    }

    /** Replaces a user's privileges with a ready-made role (STAFF or ADMIN). */
    @PostMapping("/privileges/preset")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String applyPreset(@RequestParam("userId") int userId,
                              @RequestParam("preset") String preset,
                              RedirectAttributes redirectAttributes) {
        RolePreset rolePreset;
        try {
            rolePreset = RolePreset.valueOf(preset == null ? "" : preset.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unknown role.");
            return "redirect:/admin/privileges";
        }
        if (rolePreset == RolePreset.SUPER_ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "That role cannot be assigned here.");
            return "redirect:/admin/privileges";
        }

        try {
            AppUser admin = currentUserService.getCurrentUser();
            userManagementService.applyRolePreset(userId, rolePreset, admin);
            AppUser target = userManagementService.findById(userId);
            sensitiveChangeService.reportRoleChange(admin, target,
                    rolePreset == RolePreset.ADMIN ? "Admin" : "Staff", rolePreset == RolePreset.ADMIN);
            redirectAttributes.addFlashAttribute("successMessage",
                    "\"" + target.getFullName() + "\" now has the " + rolePreset.name() + " role.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/privileges";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String listUsers(Model model) {
        model.addAttribute("users", userManagementService.findAllStaffUsers());
        return "admin/manage-users";
    }

    @GetMapping("/audit-log")
    @PreAuthorize("hasAnyAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "', '" + PermissionCode.CAN_VIEW_AUDIT_LOG + "')")
    public String viewAuditLog(Model model) {
        List<SecurityAuditLog> entries = auditLogService.findRecentEntries();
        model.addAttribute("entries", entries);
        return "admin/audit-log";
    }

    @PostMapping("/audit-log/delete/{entryId}")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String deleteAuditEntry(@PathVariable int entryId, RedirectAttributes redirectAttributes) {
        AppUser admin = currentUserService.getCurrentUser();
        auditLogService.record("AUDIT_LOG_ENTRY_DELETED",
                admin != null ? admin.getUsername() : "system",
                null,
                "Deleted audit log entry id " + entryId + ".");
        auditLogService.deleteEntry(entryId);
        redirectAttributes.addFlashAttribute("successMessage", "Audit log entry deleted.");
        return "redirect:/admin/audit-log";
    }

    @PostMapping("/audit-log/clear")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String clearAuditLog(RedirectAttributes redirectAttributes) {
        AppUser admin = currentUserService.getCurrentUser();
        auditLogService.record("AUDIT_LOG_CLEARED",
                admin != null ? admin.getUsername() : "system",
                null,
                "Administrator cleared the security audit log.");
        auditLogService.deleteAll();
        redirectAttributes.addFlashAttribute("successMessage", "Security audit log cleared.");
        return "redirect:/admin/audit-log";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String showCreateUserForm(Model model) {
        model.addAttribute("newUserForm", new NewStaffUserForm());
        model.addAttribute("employees", employeeService.findAll());
        return "admin/create-user";
    }

    @PostMapping("/users/new")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
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
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String toggleEnabled(@RequestParam("userId") int userId,
                                @RequestParam("enabled") boolean enabled,
                                RedirectAttributes redirectAttributes) {
        userManagementService.setEnabled(userId, enabled);
        redirectAttributes.addFlashAttribute("successMessage",
                enabled ? "Account enabled." : "Account disabled.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/reset-password")
    @PreAuthorize("hasAuthority('" + CustomUserDetailsService.SUPER_ADMIN_AUTHORITY + "')")
    public String resetUserPassword(@RequestParam("userId") int userId,
                                    @RequestParam("newPassword") String newPassword,
                                    @RequestParam("confirmPassword") String confirmPassword,
                                    RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/admin/users";
        }

        try {
            AppUser admin = currentUserService.getCurrentUser();
            userManagementService.resetPassword(userId, newPassword, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

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
