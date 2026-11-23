package com.rainbow.springboot.thymeleafdemo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import com.rainbow.springboot.thymeleafdemo.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Central security configuration for the application.
 *
 * BIG CHANGE from the earlier role-based version of this project:
 * authorities are no longer fixed ROLE_EMPLOYEE / ROLE_MANAGER / ROLE_ADMIN
 * values read from a generic Spring-Security-managed "authorities" table.
 * Instead:
 *
 *  - CustomUserDetailsService builds each user's authorities dynamically
 *    from individually-granted Permission rows (see UserPermission).
 *  - @EnableMethodSecurity + @PreAuthorize("hasAuthority('CAN_...')") on
 *    individual Controller methods is now the primary way access is
 *    enforced for business actions (add/edit/delete employee, etc.) -
 *    see EmployeeController in the next phase.
 *  - URL-level rules here are kept DELIBERATELY MINIMAL: just enough to
 *    (a) allow the public landing page and login page to be viewed by
 *        anyone, (b) require SOME authentication for everything else, and
 *    (c) reserve the "/admin/**" system-settings area (privilege granting)
 *        for the single Super Admin only, via the special SUPER_ADMIN
 *        authority that only ever exists for that one account.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * BCrypt is the industry-standard one-way password hashing algorithm -
     * this bean is used both when checking a submitted login password and
     * whenever the Super Admin sets/resets another user's password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tracks every currently-logged-in user's HttpSession, keyed by
     * username. This is what makes permission changes take effect
     * IMMEDIATELY rather than "the next time the user happens to log in
     * again": when the Super Admin grants or revokes a privilege,
     * UserManagementServiceImpl looks up that user's active session(s) here
     * and expires them, forcing a fresh login (and therefore a freshly
     * computed set of authorities from CustomUserDetailsService) on their
     * very next request. Without this, Spring Security would otherwise keep
     * using the authorities that were computed once, at the time the user
     * originally logged in, for the entire lifetime of their session.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Check "enabled" only AFTER the password has been verified, so the "account disabled"
        // message is shown to the real owner and never reveals which usernames exist.
        provider.setPreAuthenticationChecks(user -> { });
        provider.setPostAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("User account is disabled");
            }
        });
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider authenticationProvider,
                                           PrivilegeBasedLoginSuccessHandler loginSuccessHandler,
                                           AuditLogService auditLogService,
                                           LoginAttemptService loginAttemptService) throws Exception {

        http.authenticationProvider(authenticationProvider);
        http.addFilterBefore(new LoginThrottleFilter(loginAttemptService),
                UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(configurer -> configurer
                // Public landing page + login page + static assets: visible with NO login.
                // No employee/business data is ever served from these URLs.
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                // Staff users can be granted explicit access to view the audit log while the
                // Super Admin retains full access to the management endpoints.
                .requestMatchers("/admin/audit-log").hasAnyAuthority(
                        CustomUserDetailsService.SUPER_ADMIN_AUTHORITY,
                        PermissionCode.CAN_VIEW_AUDIT_LOG)
                .requestMatchers(HttpMethod.POST, "/admin/audit-log/delete/**", "/admin/audit-log/clear")
                .hasAuthority(CustomUserDetailsService.SUPER_ADMIN_AUTHORITY)

                // System-level settings (granting/revoking privileges, user management)
                // are reserved for the single Super Admin account only. This is enforced
                // here at the URL level AND again with @PreAuthorize on the admin
                // controller methods themselves (defense in depth).
                .requestMatchers("/admin/**").hasAuthority(CustomUserDetailsService.SUPER_ADMIN_AUTHORITY)

                // Everything else just requires SOME authenticated account;
                // the SPECIFIC business action (view/add/edit/delete/etc.) is
                // then checked per-method with @PreAuthorize based on the
                // user's individually-granted permissions.
                .anyRequest().authenticated());

        // Custom login page (a dedicated /login view, never a direct dashboard),
        // with a success handler that redirects based on the user's actual
        // granted privileges rather than a fixed role (see
        // PrivilegeBasedLoginSuccessHandler for the exact rules).
        http.formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)
                .failureHandler((request, response, exception) -> response.sendRedirect(
                        request.getContextPath() + (exception instanceof DisabledException ? "/login?disabled" : "/login?error")))
                .permitAll());

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler(auditLogService))
                .logoutSuccessUrl("/")
                .permitAll());

        // A friendly "you're not allowed to do that" page instead of a raw 403
        // when @PreAuthorize denies a request - see GlobalExceptionHandler's
        // AccessDeniedException handler, and templates/error.html.
        http.exceptionHandling(handler -> handler
                .accessDeniedPage("/access-denied"));

        // Registers every new login in the SessionRegistry (see its javadoc
        // above) and, once a session has been invalidated via that registry,
        // sends the user back to the login page with an explanatory message
        // instead of a confusing generic error.
        http.sessionManagement(session -> session
                .maximumSessions(1)
                .sessionRegistry(sessionRegistry())
                .expiredUrl("/login?expired"));

        return http.build();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(AuditLogService auditLogService) {
        return (request, response, authentication) -> {
            String username = authentication != null ? authentication.getName() : null;
            if (username != null) {
                auditLogService.record("LOGOUT", username, null,
                        "User logged out successfully.");
            }
            SecurityContextHolder.clearContext();
            response.sendRedirect("/");
        };
    }
}
