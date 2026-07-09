package com.rainbow.springboot.thymeleafdemo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Records that a specific {@link Permission} has been granted to a specific
 * {@link AppUser} - this is the "join table" of the User <-> Permission
 * many-to-many relationship, modelled as its own entity (rather than a bare
 * @ManyToMany) specifically so we can keep an audit trail:
 *
 *   - who granted it (grantedBy)
 *   - when it was granted (grantedAt)
 *
 * This is what lets the admin UI later show, e.g., "CAN_DELETE_EMPLOYEE -
 * granted by susan on 2026-07-01" next to each privilege, and is standard
 * practice in real access-control systems where accountability for who
 * granted what matters.
 *
 * Revoking a privilege is implemented as DELETING the corresponding
 * UserPermission row (see UserPermissionService in the next phase) rather
 * than a soft "revoked" flag, keeping the "currently granted permissions"
 * query trivial (just: does a row exist?).
 */
@Entity
@Table(name = "user_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_id"}))
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /** The Super Admin (or another user with delegated rights) who granted this. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private AppUser grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt = LocalDateTime.now();

    public UserPermission() {
    }

    public UserPermission(AppUser user, Permission permission, AppUser grantedBy) {
        this.user = user;
        this.permission = permission;
        this.grantedBy = grantedBy;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public AppUser getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(AppUser grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    @Override
    public String toString() {
        return "UserPermission[user=" + (user != null ? user.getUsername() : null)
                + ", permission=" + (permission != null ? permission.getCode() : null) + "]";
    }
}
