package com.rainbow.springboot.thymeleafdemo.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A login account for the system.
 *
 * This is intentionally a SEPARATE entity from {@link Employee}:
 *  - "Who can log in and with what permissions" (AppUser) is an
 *    authentication/authorization concern.
 *  - "HR data about a staff member" (Employee: salary, hire date, department...)
 *    is a business-data concern.
 *
 * A single Super Admin account, for example, does not need to correspond to
 * any Employee HR record at all. Conversely, not every Employee necessarily
 * has a login account. Where a staff member DOES need to log in, their
 * AppUser optionally links to their Employee record via {@link #employee}.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt-hashed password - the plain-text password is never stored. */
    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank(message = "Full name is required")
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Email(message = "Please provide a valid email address")
    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * True for exactly one row in the whole system: the single Super Admin.
     * This flag - not a "ROLE_ADMIN" authority - is what the Super Admin
     * gate (see SecurityConfig / SuperAdminOnly checks) actually tests,
     * which is what guarantees there can only ever be one super admin no
     * matter what permissions happen to be granted to other users.
     */
    @Column(name = "super_admin", nullable = false)
    private boolean superAdmin = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Optional link to this user's HR record. Nullable - the Super Admin
     * account, for instance, will not have one.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    /**
     * Every permission individually granted to this user (see UserPermission).
     * mappedBy = "user" means the foreign key lives on the UserPermission
     * side, not here - see the note in Department.java for the same pattern.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserPermission> grantedPermissions = new HashSet<>();

    public AppUser() {
    }

    public AppUser(String username, String password, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Set<UserPermission> getGrantedPermissions() {
        return grantedPermissions;
    }

    public void setGrantedPermissions(Set<UserPermission> grantedPermissions) {
        this.grantedPermissions = grantedPermissions;
    }

    @Override
    public String toString() {
        return "AppUser[id=" + id + ", username=" + username + ", superAdmin=" + superAdmin + "]";
    }
}
