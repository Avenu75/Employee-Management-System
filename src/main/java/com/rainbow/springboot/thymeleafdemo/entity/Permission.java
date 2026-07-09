package com.rainbow.springboot.thymeleafdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single, fine-grained, grantable action in the system, e.g. "CAN_ADD_EMPLOYEE".
 *
 * This is deliberately NOT an enum in Java, even though the set of possible
 * permissions is fixed for now (see {@link PermissionCode} for the fixed
 * catalog of codes). Keeping Permission as a database-backed entity - rather
 * than hard-coding roles into Spring Security config - means:
 *
 *  1. New permissions can be added by inserting a new row (see data.sql),
 *     without touching any Java/Spring Security configuration code.
 *  2. The admin UI can list "all known permissions" straight from the
 *     database and let the Super Admin tick/untick them per user.
 *  3. Each Permission's "code" (e.g. CAN_ADD_EMPLOYEE) becomes a Spring
 *     Security "authority" string at login time (see CustomUserDetailsService),
 *     which is what @PreAuthorize("hasAuthority('CAN_ADD_EMPLOYEE')") checks
 *     against.
 */
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    /**
     * The stable machine-readable code used as the Spring Security authority
     * string, e.g. "CAN_ADD_EMPLOYEE". Convention: SCREAMING_SNAKE_CASE,
     * always starting with CAN_ to make its purpose obvious at a glance.
     */
    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    /** Short human-readable label shown in the admin "grant privilege" UI. */
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    /** Longer explanation shown as help text next to the checkbox in the UI. */
    @Column(name = "description", length = 255)
    private String description;

    /** Logical grouping used to organise the grant-privilege form into sections. */
    @Column(name = "category", length = 60)
    private String category;

    public Permission() {
    }

    public Permission(String code, String displayName, String description, String category) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Permission[code=" + code + "]";
    }
}
