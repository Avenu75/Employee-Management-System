package com.rainbow.springboot.thymeleafdemo.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Decides which page to land on immediately after a successful login,
 * based on what the user is actually allowed to do - NOT a fixed role.
 *
 * Redirect rules (checked in this order, first match wins):
 *   1. SUPER_ADMIN authority             -> /admin/dashboard
 *   2. Any CAN_* management permission   -> /dashboard  (the staff/manager view)
 *   3. Nobody special (plain employee, no granted permissions yet)
 *                                         -> /profile    (self-service view only)
 *
 * This directly implements requirement 4 ("Login Flow & Redirects") from
 * the industry-readiness upgrade: admins go to the admin dashboard, users
 * with granted privileges go to their permitted management views, and
 * plain employees only ever see their own profile.
 */
@Component
public class PrivilegeBasedLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /**
     * Any one of these being present is enough to be treated as "has some
     * management capability" and therefore land on the staff dashboard
     * rather than the bare self-service profile page.
     */
    private static final Set<String> MANAGEMENT_AUTHORITIES = Set.of(
            PermissionCode.CAN_VIEW_EMPLOYEE,
            PermissionCode.CAN_ADD_EMPLOYEE,
            PermissionCode.CAN_EDIT_EMPLOYEE,
            PermissionCode.CAN_DELETE_EMPLOYEE,
            PermissionCode.CAN_VIEW_DEPARTMENT,
            PermissionCode.CAN_MANAGE_DEPARTMENT,
            PermissionCode.CAN_VIEW_REPORTS,
            PermissionCode.CAN_MANAGE_ATTENDANCE);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        boolean isSuperAdmin = false;
        boolean hasManagementPermission = false;
        boolean hasReception = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (CustomUserDetailsService.SUPER_ADMIN_AUTHORITY.equals(value)) {
                isSuperAdmin = true;
                break;
            }
            if (PermissionCode.CAN_USE_RECEPTION.equals(value)) {
                hasReception = true;
            }
            if (MANAGEMENT_AUTHORITIES.contains(value)) {
                hasManagementPermission = true;
            }
        }

        if (isSuperAdmin) {
            return "/admin/dashboard";
        }
        if (hasManagementPermission) {
            return "/dashboard";
        }
        if (hasReception) {
            return "/reception";
        }
        return "/profile";
    }
}
