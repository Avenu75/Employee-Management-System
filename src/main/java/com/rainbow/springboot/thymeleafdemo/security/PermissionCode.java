package com.rainbow.springboot.thymeleafdemo.security;

/**
 * Central catalog of every known permission "code" string used across the
 * application.
 *
 * IMPORTANT: this class does not grant anything by itself - it only holds
 * constants so that:
 *   (a) the database seeder (DataInitializer) knows which Permission rows
 *       must exist, and
 *   (b) Java code (e.g. @PreAuthorize("hasAuthority(PermissionCode.CAN_ADD_EMPLOYEE)"))
 *       never has to repeat a raw, typo-prone string literal.
 *
 * Adding a brand-new permission still requires one new constant here (so the
 * compiler can catch typos in @PreAuthorize checks) plus one new row in the
 * permission table - but it never requires touching SecurityConfig or any
 * role logic, which is the key benefit of an action-based permission model
 * over a fixed-role model.
 */
public final class PermissionCode {

    private PermissionCode() {
        // constants only, never instantiated
    }

    public static final String CAN_VIEW_OWN_PROFILE = "CAN_VIEW_OWN_PROFILE";
    public static final String CAN_EDIT_OWN_PROFILE = "CAN_EDIT_OWN_PROFILE";
    public static final String CAN_UPLOAD_PROFILE_IMAGE = "CAN_UPLOAD_PROFILE_IMAGE";

    public static final String CAN_VIEW_EMPLOYEE = "CAN_VIEW_EMPLOYEE";
    public static final String CAN_ADD_EMPLOYEE = "CAN_ADD_EMPLOYEE";
    public static final String CAN_EDIT_EMPLOYEE = "CAN_EDIT_EMPLOYEE";
    public static final String CAN_DELETE_EMPLOYEE = "CAN_DELETE_EMPLOYEE";

    public static final String CAN_VIEW_AUDIT_LOG = "CAN_VIEW_AUDIT_LOG";
    public static final String CAN_MANAGE_AUDIT_LOG = "CAN_MANAGE_AUDIT_LOG";

    public static final String CAN_VIEW_DEPARTMENT = "CAN_VIEW_DEPARTMENT";
    public static final String CAN_MANAGE_DEPARTMENT = "CAN_MANAGE_DEPARTMENT";

    public static final String CAN_VIEW_REPORTS = "CAN_VIEW_REPORTS";
    public static final String CAN_MANAGE_ATTENDANCE = "CAN_MANAGE_ATTENDANCE";

    public static final String CAN_MANAGE_LEAVE = "CAN_MANAGE_LEAVE";
    public static final String CAN_VIEW_PAYROLL = "CAN_VIEW_PAYROLL";
    /** Set up tax, deductions and adjustments, then generate and finalise a month. */
    public static final String CAN_MANAGE_PAYROLL = "CAN_MANAGE_PAYROLL";

    public static final String CAN_MANAGE_ANNOUNCEMENTS = "CAN_MANAGE_ANNOUNCEMENTS";
    public static final String CAN_USE_RECEPTION = "CAN_USE_RECEPTION";
    /** Lets reception staff read the Administration announcements (managers and ordinary staff already can). */
    public static final String CAN_VIEW_ADMIN_ANNOUNCEMENTS = "CAN_VIEW_ADMIN_ANNOUNCEMENTS";

    public static final String CAN_MANAGE_USERS = "CAN_MANAGE_USERS";
    public static final String CAN_RESET_PASSWORD = "CAN_RESET_PASSWORD";
    public static final String CAN_MANAGE_PERMISSIONS = "CAN_MANAGE_PERMISSIONS";

    /**
     * Full ordered list, used by the DataInitializer seeder to make sure
     * every one of these exists as a Permission row at startup.
     * Kept as an array (not enum) deliberately - see Permission.java javadoc.
     */
    public static final String[] ALL_CODES = {
            CAN_VIEW_OWN_PROFILE,
            CAN_EDIT_OWN_PROFILE,
            CAN_UPLOAD_PROFILE_IMAGE,
            CAN_VIEW_EMPLOYEE,
            CAN_ADD_EMPLOYEE,
            CAN_EDIT_EMPLOYEE,
            CAN_DELETE_EMPLOYEE,
            CAN_VIEW_AUDIT_LOG,
            CAN_MANAGE_AUDIT_LOG,
            CAN_VIEW_DEPARTMENT,
            CAN_MANAGE_DEPARTMENT,
            CAN_VIEW_REPORTS,
            CAN_MANAGE_ATTENDANCE,
            CAN_MANAGE_LEAVE,
            CAN_VIEW_PAYROLL,
            CAN_MANAGE_PAYROLL,
            CAN_MANAGE_ANNOUNCEMENTS,
            CAN_USE_RECEPTION,
            CAN_VIEW_ADMIN_ANNOUNCEMENTS,
            CAN_MANAGE_USERS,
            CAN_RESET_PASSWORD,
            CAN_MANAGE_PERMISSIONS
    };
}
