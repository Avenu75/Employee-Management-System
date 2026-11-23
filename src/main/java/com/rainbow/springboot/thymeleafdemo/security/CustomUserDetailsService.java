package com.rainbow.springboot.thymeleafdemo.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.UserPermissionRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.UserPermission;

/**
 * Replaces Spring Security's default {@code JdbcUserDetailsManager} (which
 * expected a fixed "users"/"authorities" table pair populated with static
 * ROLE_* values).
 *
 * Instead, this class builds each user's list of Spring Security
 * "authorities" DYNAMICALLY, at login time, from whatever rows currently
 * exist in user_permission for that user. This is exactly what makes the
 * system "action-based" rather than "role-based": granting or revoking a
 * single Permission row in the database immediately changes what a user can
 * do, with zero Java code changes and zero redeploys.
 *
 * Two kinds of authorities are produced for a given user:
 *
 *   1. One authority string per individually-granted Permission, e.g.
 *      "CAN_ADD_EMPLOYEE" - checked by
 *      {@code @PreAuthorize("hasAuthority('CAN_ADD_EMPLOYEE')")}.
 *
 *   2. For the single Super Admin only, an additional "SUPER_ADMIN"
 *      authority - checked by
 *      {@code @PreAuthorize("hasAuthority('SUPER_ADMIN')")} to gate
 *      system-level settings (like the privilege-granting screen itself).
 *      This is deliberately kept SEPARATE from the business-action
 *      permissions above: being able to grant/revoke privileges is a
 *      system-administration capability, not a business action like
 *      "add an employee", and must never be grantable to anyone but the
 *      one true Super Admin (see AppUser.superAdmin and
 *      SuperAdminInitializer).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    public static final String SUPER_ADMIN_AUTHORITY = "SUPER_ADMIN";
    /**
     * Taking other people's attendance is a duty for staff the Super Admin delegates it to.
     * The Super Admin account itself never exercises these, even though it holds every
     * permission row; it keeps only its own clock in and clock out.
     */
    private static final Set<String> DELEGATED_DUTIES = Set.of(
            PermissionCode.CAN_MANAGE_ATTENDANCE, PermissionCode.CAN_USE_RECEPTION);

    private final AppUserRepository appUserRepository;
    private final UserPermissionRepository userPermissionRepository;

    public CustomUserDetailsService(AppUserRepository theAppUserRepository,
                                    UserPermissionRepository theUserPermissionRepository) {
        appUserRepository = theAppUserRepository;
        userPermissionRepository = theUserPermissionRepository;
    }

    /**
     * @Transactional is required here: AppUser.grantedPermissions and each
     * UserPermission.permission are LAZY-loaded associations. Without an
     * active Hibernate session for the whole duration of this method,
     * accessing permission.getCode() below would throw
     * LazyInitializationException once the session used to fetch the
     * AppUser itself has already closed.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for username: " + username));

        List<GrantedAuthority> authorities = buildAuthorities(appUser);

        return new User(
                appUser.getUsername(),
                appUser.getPassword(),
                appUser.isEnabled(),
                true,   // accountNonExpired
                true,   // credentialsNonExpired
                true,   // accountNonLocked
                authorities);
    }

    private List<GrantedAuthority> buildAuthorities(AppUser appUser) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (appUser.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority(SUPER_ADMIN_AUTHORITY));
        }

        List<UserPermission> granted = userPermissionRepository.findByUser(appUser);
        for (UserPermission up : granted) {
            String code = up.getPermission().getCode();
            if (appUser.isSuperAdmin() && DELEGATED_DUTIES.contains(code)) {
                continue;
            }
            authorities.add(new SimpleGrantedAuthority(code));
        }

        return authorities;
    }
}
