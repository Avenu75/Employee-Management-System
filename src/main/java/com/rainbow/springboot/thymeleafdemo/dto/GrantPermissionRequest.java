package com.rainbow.springboot.thymeleafdemo.dto;

/**
 * Simple form-binding object for the "grant privilege" modal on the admin
 * dashboard - carries which user and which permission the Super Admin just
 * selected. Kept as a tiny standalone DTO rather than binding straight to
 * the AppUser or Permission entities, since this form never needs (or
 * should be able to set) any other field on either entity.
 */
public class GrantPermissionRequest {

    private int userId;
    private int permissionId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }
}
