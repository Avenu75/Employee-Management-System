package com.rainbow.springboot.thymeleafdemo.security;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Named role presets used to map a user to a consistent set of permissions.
 *
 * This is a backend convenience layer on top of the existing permission-based
 * authorization model. Instead of manually granting one permission at a time,
 * the application can apply a whole preset such as ADMIN or STAFF.
 */
public enum RolePreset {
    SUPER_ADMIN(
            PermissionCode.CAN_VIEW_EMPLOYEE,
            PermissionCode.CAN_ADD_EMPLOYEE,
            PermissionCode.CAN_EDIT_EMPLOYEE,
            PermissionCode.CAN_DELETE_EMPLOYEE,
            PermissionCode.CAN_VIEW_AUDIT_LOG,
            PermissionCode.CAN_MANAGE_AUDIT_LOG,
            PermissionCode.CAN_VIEW_DEPARTMENT,
            PermissionCode.CAN_MANAGE_DEPARTMENT,
            PermissionCode.CAN_VIEW_REPORTS,
            PermissionCode.CAN_MANAGE_LEAVE,
            PermissionCode.CAN_VIEW_PAYROLL,
            PermissionCode.CAN_MANAGE_PAYROLL,
            PermissionCode.CAN_MANAGE_ANNOUNCEMENTS,
            PermissionCode.CAN_VIEW_OWN_PROFILE,
            PermissionCode.CAN_EDIT_OWN_PROFILE,
            PermissionCode.CAN_UPLOAD_PROFILE_IMAGE,
            PermissionCode.CAN_MANAGE_USERS,
            PermissionCode.CAN_RESET_PASSWORD,
            PermissionCode.CAN_MANAGE_PERMISSIONS),

    ADMIN(
            PermissionCode.CAN_VIEW_EMPLOYEE,
            PermissionCode.CAN_ADD_EMPLOYEE,
            PermissionCode.CAN_EDIT_EMPLOYEE,
            PermissionCode.CAN_DELETE_EMPLOYEE,
            PermissionCode.CAN_VIEW_AUDIT_LOG,
            PermissionCode.CAN_MANAGE_AUDIT_LOG,
            PermissionCode.CAN_VIEW_DEPARTMENT,
            PermissionCode.CAN_MANAGE_DEPARTMENT,
            PermissionCode.CAN_VIEW_REPORTS,
            PermissionCode.CAN_MANAGE_LEAVE,
            PermissionCode.CAN_VIEW_PAYROLL,
            PermissionCode.CAN_MANAGE_ANNOUNCEMENTS,
            PermissionCode.CAN_VIEW_OWN_PROFILE,
            PermissionCode.CAN_EDIT_OWN_PROFILE,
            PermissionCode.CAN_UPLOAD_PROFILE_IMAGE),

    /** Front-desk staff: take attendance of other workers at the reception gate. */
    RECEPTION(
            PermissionCode.CAN_USE_RECEPTION,
            PermissionCode.CAN_VIEW_OWN_PROFILE,
            PermissionCode.CAN_EDIT_OWN_PROFILE,
            PermissionCode.CAN_UPLOAD_PROFILE_IMAGE),

    STAFF(
            PermissionCode.CAN_VIEW_OWN_PROFILE,
            PermissionCode.CAN_EDIT_OWN_PROFILE,
            PermissionCode.CAN_UPLOAD_PROFILE_IMAGE);

    private final Set<String> permissions;

    RolePreset(String... permissionCodes) {
        this.permissions = new LinkedHashSet<>();
        for (String code : permissionCodes) {
            this.permissions.add(code);
        }
    }

    public Set<String> getPermissions() {
        return Set.copyOf(permissions);
    }
}
