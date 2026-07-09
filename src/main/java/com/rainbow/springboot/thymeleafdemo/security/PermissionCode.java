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

    public static final String CAN_VIEW_EMPLOYEE = "CAN_VIEW_EMPLOYEE";
    public static final String CAN_ADD_EMPLOYEE = "CAN_ADD_EMPLOYEE";
    public static final String CAN_EDIT_EMPLOYEE = "CAN_EDIT_EMPLOYEE";
    public static final String CAN_DELETE_EMPLOYEE = "CAN_DELETE_EMPLOYEE";

    public static final String CAN_VIEW_DEPARTMENT = "CAN_VIEW_DEPARTMENT";
    public static final String CAN_MANAGE_DEPARTMENT = "CAN_MANAGE_DEPARTMENT";

    public static final String CAN_VIEW_REPORTS = "CAN_VIEW_REPORTS";
    public static final String CAN_MANAGE_ATTENDANCE = "CAN_MANAGE_ATTENDANCE";

    /**
     * Full ordered list, used by the DataInitializer seeder to make sure
     * every one of these exists as a Permission row at startup.
     * Kept as an array (not enum) deliberately - see Permission.java javadoc.
     */
    public static final String[] ALL_CODES = {
            CAN_VIEW_EMPLOYEE,
            CAN_ADD_EMPLOYEE,
            CAN_EDIT_EMPLOYEE,
            CAN_DELETE_EMPLOYEE,
            CAN_VIEW_DEPARTMENT,
            CAN_MANAGE_DEPARTMENT,
            CAN_VIEW_REPORTS,
            CAN_MANAGE_ATTENDANCE
    };
}
