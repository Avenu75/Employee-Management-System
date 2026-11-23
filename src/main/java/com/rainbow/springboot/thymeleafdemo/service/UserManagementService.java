package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;
import java.util.Set;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Permission;
import com.rainbow.springboot.thymeleafdemo.security.RolePreset;

/**
 * Business logic for managing login accounts (AppUser) and the individual
 * permissions granted to them.
 *
 * This is the ONLY service that is allowed to create AppUser accounts or
 * change their granted permissions - Controllers never touch the
 * repositories directly. Critically, this service also enforces the
 * "single super admin" rule at the business-logic layer (not just via the
 * one-time DataInitializer): nobody, including the super admin themself,
 * can use this service to create a second super admin account or to grant
 * SUPER_ADMIN-equivalent power to anyone else.
 */
public interface UserManagementService {

    List<AppUser> findAllStaffUsers();

    AppUser findById(int id);

    AppUser findByUsername(String username);

    /** Creates a new staff login account (never a super admin - see class javadoc). */
    AppUser createStaffUser(String username, String rawPassword, String fullName, String email, Integer employeeId);

    void setEnabled(int userId, boolean enabled);

    void resetPassword(int userId, String newPassword, AppUser changedByUser);

    void changePassword(AppUser currentUser, String currentPassword, String newPassword);

    /** All permissions currently granted to this user, e.g. for rendering checkboxes. */
    Set<Permission> getGrantedPermissions(AppUser user);

    /** Returns true if this user currently has the named permission or is the Super Admin. */
    boolean hasPermission(AppUser user, String permissionCode);

    /** Applies a named preset to a user, replacing their current permission set with the preset. */
    void applyRolePreset(int targetUserId, RolePreset rolePreset, AppUser changedByUser);

    /**
     * Grants the given permission to the given user, recording who granted
     * it (audit trail). Does nothing (idempotent) if already granted.
     */
    void grantPermission(int targetUserId, int permissionId, AppUser grantedByUser);

    /** Revokes the given permission from the given user. Does nothing if not currently granted. */
    void revokePermission(int targetUserId, int permissionId);

    List<Permission> findAllPermissions();
}
