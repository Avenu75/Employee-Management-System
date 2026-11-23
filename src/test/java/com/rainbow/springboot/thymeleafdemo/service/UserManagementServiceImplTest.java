package com.rainbow.springboot.thymeleafdemo.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PermissionRepository;
import com.rainbow.springboot.thymeleafdemo.dao.UserPermissionRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Permission;

import java.util.Collections;
import java.util.Optional;

/**
 * Verifies the "single Super Admin" business rule is enforced by the
 * Service layer itself (not just the one-time DataInitializer seeder):
 * nobody can ever use UserManagementService to grant/revoke permissions
 * for, or disable, the Super Admin account.
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private UserPermissionRepository userPermissionRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SessionRegistry sessionRegistry;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private AppUser superAdmin;
    private AppUser normalUser;

    @BeforeEach
    void setUp() {
        superAdmin = new AppUser("superadmin", "hash", "System Administrator", "admin@acme.local");
        superAdmin.setId(1);
        superAdmin.setSuperAdmin(true);

        normalUser = new AppUser("mgr_monica", "hash", "Monica Geller", "monica@company.com");
        normalUser.setId(2);
        normalUser.setSuperAdmin(false);

        lenient().when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.emptyList());
    }

    @Test
    void grantPermission_refusesToModifySuperAdmin() {
        when(appUserRepository.findById(1)).thenReturn(Optional.of(superAdmin));

        assertThrows(IllegalArgumentException.class, () ->
                userManagementService.grantPermission(1, 99, superAdmin));
    }

    @Test
    void setEnabled_refusesToDisableSuperAdmin() {
        when(appUserRepository.findById(1)).thenReturn(Optional.of(superAdmin));

        assertThrows(IllegalArgumentException.class, () ->
                userManagementService.setEnabled(1, false));
    }

    @Test
    void grantPermission_succeedsForNormalUser() {
        Permission permission = new Permission("CAN_VIEW_EMPLOYEE", "View Employee", "desc", "Employee Management");
        permission.setId(5);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(permissionRepository.findById(5)).thenReturn(Optional.of(permission));
        when(userPermissionRepository.findByUserAndPermission(normalUser, permission)).thenReturn(Optional.empty());

        userManagementService.grantPermission(2, 5, superAdmin);

        verify(userPermissionRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createStaffUser_neverSetsSuperAdminFlag() {
        when(appUserRepository.existsByUsername("newstaff")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(appUserRepository.save(org.mockito.ArgumentMatchers.any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppUser created = userManagementService.createStaffUser("newstaff", "pw", "New Staff", "n@x.com", null);

        // The createStaffUser method signature has no parameter for superAdmin at all,
        // so this asserts the structural guarantee: it always defaults to false.
        org.junit.jupiter.api.Assertions.assertFalse(created.isSuperAdmin());
    }
}
