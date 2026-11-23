package com.rainbow.springboot.thymeleafdemo.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.rainbow.springboot.thymeleafdemo.service.AuditLogService;

@Component
public class SecurityAuditEventListener {

    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;

    public SecurityAuditEventListener(AuditLogService theAuditLogService,
                                      LoginAttemptService theLoginAttemptService) {
        auditLogService = theAuditLogService;
        loginAttemptService = theLoginAttemptService;
    }

    private static String remoteAddress(org.springframework.security.core.Authentication auth) {
        Object details = auth.getDetails();
        return details instanceof WebAuthenticationDetails
                ? ((WebAuthenticationDetails) details).getRemoteAddress() : null;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.reset(username);
        auditLogService.record("LOGIN_SUCCESS", username, null,
                "User logged in successfully.");
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.recordFailure(username);
        String ip = remoteAddress(event.getAuthentication());
        if (ip != null) {
            loginAttemptService.recordFailure("ip:" + ip);
        }
        auditLogService.record("LOGIN_FAILED", username, null,
                "Failed login attempt with invalid credentials.");
    }
}
