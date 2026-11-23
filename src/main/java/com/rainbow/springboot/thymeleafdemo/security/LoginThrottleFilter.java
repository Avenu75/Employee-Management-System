package com.rainbow.springboot.thymeleafdemo.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects login POSTs from a username or IP address that has exceeded the
 * allowed number of failed attempts. Registered inside the Spring Security
 * filter chain (see SecurityConfig); it is intentionally NOT a @Component so
 * Spring Boot does not also register it as a plain servlet filter.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;

    public LoginThrottleFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String username = request.getParameter("username");
        String ip = request.getRemoteAddr();
        if (loginAttemptService.isBlocked(username) || loginAttemptService.isBlocked("ip:" + ip)) {
            response.sendRedirect(request.getContextPath() + "/login?locked");
            return;
        }
        chain.doFilter(request, response);
    }
}
