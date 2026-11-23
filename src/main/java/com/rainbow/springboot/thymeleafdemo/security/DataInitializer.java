package com.rainbow.springboot.thymeleafdemo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PermissionRepository;
import com.rainbow.springboot.thymeleafdemo.dao.UserPermissionRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Permission;
import com.rainbow.springboot.thymeleafdemo.entity.UserPermission;

/**
 * Runs once, automatically, every time the application starts up
 * (Spring Boot calls every {@link CommandLineRunner} bean's run() method
 * right after the application context has finished loading).
 *
 * Two responsibilities, both fully IDEMPOTENT (safe to run on every single
 * restart without creating duplicates):
 *
 *  1. Seed the fixed catalog of {@link Permission} rows from
 *     {@link PermissionCode#ALL_CODES}, so the "grant privilege" UI always
 *     has a complete, up-to-date list of grantable actions to show,
 *     matching exactly what the @PreAuthorize checks in the code expect.
 *
 *  2. Seed EXACTLY ONE Super Admin {@link AppUser} account, the very first
 *     time the application ever starts against a fresh database.
 *     - There is intentionally NO signup/registration form anywhere in this
 *       application that can create a super admin (or any account at all) -
 *       this startup runner is the ONLY code path that can ever set
 *       superAdmin = true, and it refuses to run again once one already
 *       exists (see the existsBySuperAdminTrue() guard below). This is what
 *       fulfils the "single super admin" requirement at the architecture
 *       level, rather than relying on UI restrictions alone.
 *
 * The initial super admin credentials are read from environment variables
 * (with safe local-development defaults), for the same reason database
 * credentials are - see application.properties - so a real deployment never
 * needs the default password hard-coded anywhere.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permissionRepository;
    private final AppUserRepository appUserRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.username:superadmin}")
    private String superAdminUsername;

    @Value("${app.super-admin.password:ChangeMe123!}")
    private String superAdminPassword;

    @Value("${app.super-admin.full-name:System Administrator}")
    private String superAdminFullName;

    @Value("${app.super-admin.email:admin@acme.local}")
    private String superAdminEmail;

    public DataInitializer(PermissionRepository thePermissionRepository,
                           AppUserRepository theAppUserRepository,
                           UserPermissionRepository theUserPermissionRepository,
                           PasswordEncoder thePasswordEncoder) {
        permissionRepository = thePermissionRepository;
        appUserRepository = theAppUserRepository;
        userPermissionRepository = theUserPermissionRepository;
        passwordEncoder = thePasswordEncoder;
    }

    @Override
    public void run(String... args) {
        seedPermissionCatalog();
        seedSingleSuperAdmin();
    }

    private void seedPermissionCatalog() {
        for (String code : PermissionCode.ALL_CODES) {
            if (!permissionRepository.existsByCode(code)) {
                Permission permission = new Permission(
                        code,
                        toDisplayName(code),
                        "Allows this user to perform: " + toDisplayName(code).toLowerCase(),
                        categoryFor(code));
                permissionRepository.save(permission);
                log.info("Seeded permission: {}", code);
            }
        }
    }

    private void seedSingleSuperAdmin() {
        if (appUserRepository.existsBySuperAdminTrue()) {
            // A super admin already exists - never create a second one,
            // and never touch the existing one's credentials on restart.
            return;
        }

        if (appUserRepository.existsByUsername(superAdminUsername)) {
            log.warn("A user named '{}' already exists but is not marked super admin. " +
                    "Skipping super admin seeding to avoid overwriting an existing account. " +
                    "Set a different app.super-admin.username if this is unintended.", superAdminUsername);
            return;
        }

        AppUser superAdmin = new AppUser();
        superAdmin.setUsername(superAdminUsername);
        superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
        superAdmin.setFullName(superAdminFullName);
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setEnabled(true);
        superAdmin.setSuperAdmin(true);

        appUserRepository.save(superAdmin);

        for (Permission permission : permissionRepository.findAll()) {
            if (userPermissionRepository.findByUserAndPermission(superAdmin, permission).isEmpty()) {
                userPermissionRepository.save(new UserPermission(superAdmin, permission, superAdmin));
            }
        }

        log.warn("=================================================================");
        log.warn(" Seeded the ONE super admin account for this system.");
        log.warn(" Username: {}", superAdminUsername);
        log.warn(" This account has the complete role preset and all permissions");
        log.warn(" Please log in and change the password immediately if this is");
        log.warn(" the default value (set via app.super-admin.password).");
        log.warn("=================================================================");
    }

    private String toDisplayName(String code) {
        // e.g. "CAN_ADD_EMPLOYEE" -> "Add Employee"
        String withoutPrefix = code.startsWith("CAN_") ? code.substring(4) : code;
        String[] words = withoutPrefix.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.charAt(0)).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String categoryFor(String code) {
        if (code.contains("EMPLOYEE")) return "Employee Management";
        if (code.contains("AUDIT") || code.contains("LOG")) return "Security and Compliance";
        if (code.contains("DEPARTMENT")) return "Department Management";
        if (code.contains("REPORT")) return "Reporting";
        if (code.contains("ATTENDANCE")) return "Attendance";
        if (code.contains("LEAVE")) return "Leave";
        if (code.contains("PAYROLL")) return "Payroll";
        if (code.contains("ANNOUNCEMENT")) return "Communication";
        if (code.contains("RECEPTION")) return "Reception";
        return "General";
    }
}
