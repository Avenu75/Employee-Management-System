package com.rainbow.springboot.thymeleafdemo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A temporary, low-level "smoke test" that exercises the new security model
 * end-to-end WITHOUT going through HTTP (i.e. without needing a /login page
 * to exist yet). This proves CustomUserDetailsService + the password encoder
 * + DaoAuthenticationProvider are wired together correctly, ahead of the
 * controller/template phase.
 *
 * Requires a real database connection (it's a @SpringBootTest), and assumes
 * DataInitializer has already run once against that database so the seeded
 * super admin (username/password from application.properties defaults or
 * the SUPER_ADMIN_* environment variables) exists.
 */
@SpringBootTest
class AuthenticationSmokeTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private DaoAuthenticationProvider authenticationProvider;

    @Test
    void loadUserByUsername_returnsSuperAdminWithSuperAdminAuthority() {
        UserDetails user = userDetailsService.loadUserByUsername("superadmin");

        assertTrue(user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(CustomUserDetailsService.SUPER_ADMIN_AUTHORITY)));
    }

    @Test
    void authenticate_succeedsWithCorrectPassword() {
        Authentication result = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("superadmin", "ChangeMe123!"));

        assertTrue(result.isAuthenticated());
        assertEquals("superadmin", result.getName());
    }

    @Test
    void authenticate_failsWithWrongPassword() {
        assertThrows(BadCredentialsException.class, () ->
                authenticationProvider.authenticate(
                        new UsernamePasswordAuthenticationToken("superadmin", "totally-wrong-password")));
    }
}
