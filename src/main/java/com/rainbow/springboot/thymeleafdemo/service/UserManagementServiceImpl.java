package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PermissionRepository;
import com.rainbow.springboot.thymeleafdemo.dao.UserPermissionRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.Permission;
import com.rainbow.springboot.thymeleafdemo.entity.UserPermission;
import com.rainbow.springboot.thymeleafdemo.exception.DuplicateUsernameException;
import com.rainbow.springboot.thymeleafdemo.exception.EmployeeNotFoundException;
import com.rainbow.springboot.thymeleafdemo.exception.PermissionNotFoundException;
import com.rainbow.springboot.thymeleafdemo.exception.UserNotFoundException;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    public UserManagementServiceImpl(AppUserRepository theAppUserRepository,
                                      PermissionRepository thePermissionRepository,
                                      UserPermissionRepository theUserPermissionRepository,
                                      EmployeeRepository theEmployeeRepository,
                                      PasswordEncoder thePasswordEncoder,
                                      SessionRegistry theSessionRegistry) {
        appUserRepository = theAppUserRepository;
        permissionRepository = thePermissionRepository;
        userPermissionRepository = theUserPermissionRepository;
        employeeRepository = theEmployeeRepository;
        passwordEncoder = thePasswordEncoder;
        sessionRegistry = theSessionRegistry;
    }

    /**
     * Forces the given user's current session (if any) to expire, so their
     * next request is redirected to log in again. Spring Security then
     * calls CustomUserDetailsService fresh at that point, picking up
     * whatever permissions are granted RIGHT NOW rather than whatever was
     * true at the time they originally logged in. This is what makes
     * granting/revoking a privilege, or disabling an account, take effect
     * immediately instead of "next time they happen to log in".
     */
    private void forceReauthentication(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String principalUsername = principal instanceof org.springframework.security.core.userdetails.User u
                    ? u.getUsername() : principal.toString();
            if (principalUsername.equals(username)) {
                for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                    info.expireNow();
                }
            }
        }
    }

    @Override
    public List<AppUser> findAllStaffUsers() {
        // Deliberately excludes the super admin - see AppUserRepository javadoc.
        return appUserRepository.findBySuperAdminFalseOrderByFullNameAsc();
    }

    @Override
    public AppUser findById(int id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Did not find user id - " + id));
    }

    @Override
    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Did not find user - " + username));
    }

    @Override
    public AppUser createStaffUser(String username, String rawPassword, String fullName, String email, Integer employeeId) {
        if (appUserRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException("Username \"" + username + "\" is already taken.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setEnabled(true);
        // NOTE: superAdmin is never set here - this method has no parameter
        // for it at all, which is what makes it structurally impossible for
        // this service to ever create a second super admin (see class javadoc).

        if (employeeId != null) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Did not find employee id - " + employeeId));
            user.setEmployee(employee);
        }

        AppUser saved = appUserRepository.save(user);
        log.info("Created staff login account: {}", saved.getUsername());
        return saved;
    }

    @Override
    public void setEnabled(int userId, boolean enabled) {
        AppUser user = findById(userId);
        if (user.isSuperAdmin()) {
            // Defense in depth: even though the UI never offers this action for
            // the super admin, the service itself refuses to disable that account.
            throw new IllegalArgumentException("The super admin account cannot be disabled.");
        }
        user.setEnabled(enabled);
        appUserRepository.save(user);
        forceReauthentication(user.getUsername());
        log.info("User {} enabled={}", user.getUsername(), enabled);
    }

    @Override
    public Set<Permission> getGrantedPermissions(AppUser user) {
        return userPermissionRepository.findByUser(user).stream()
                .map(UserPermission::getPermission)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void grantPermission(int targetUserId, int permissionId, AppUser grantedByUser) {
        AppUser targetUser = findById(targetUserId);

        if (targetUser.isSuperAdmin()) {
            // The super admin's access is not managed through granted Permission
            // rows at all (see CustomUserDetailsService) - granting/revoking
            // individual permissions for that account is a no-op by design.
            throw new IllegalArgumentException("Cannot modify permissions for the super admin account.");
        }

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Did not find permission id - " + permissionId));

        Optional<UserPermission> existing = userPermissionRepository.findByUserAndPermission(targetUser, permission);
        if (existing.isPresent()) {
            return; // already granted - idempotent, nothing to do
        }

        UserPermission userPermission = new UserPermission(targetUser, permission, grantedByUser);
        userPermissionRepository.save(userPermission);
        forceReauthentication(targetUser.getUsername());

        log.info("Permission {} granted to {} by {}", permission.getCode(), targetUser.getUsername(),
                grantedByUser != null ? grantedByUser.getUsername() : "unknown");
    }

    @Override
    @Transactional
    public void revokePermission(int targetUserId, int permissionId) {
        AppUser targetUser = findById(targetUserId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException("Did not find permission id - " + permissionId));

        userPermissionRepository.deleteByUserAndPermission(targetUser, permission);
        forceReauthentication(targetUser.getUsername());

        log.info("Permission {} revoked from {}", permission.getCode(), targetUser.getUsername());
    }

    @Override
    public List<Permission> findAllPermissions() {
        return permissionRepository.findAllByOrderByCategoryAscDisplayNameAsc();
    }
}
