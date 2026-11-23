package com.rainbow.springboot.thymeleafdemo.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.rainbow.springboot.thymeleafdemo.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class SecurityAuditAspect {

    private final AuditLogService auditLogService;

    public SecurityAuditAspect(AuditLogService theAuditLogService) {
        auditLogService = theAuditLogService;
    }

    @Pointcut("execution(* com.rainbow.springboot.thymeleafdemo.controller..*(..))")
    public void controllerMethods() {
        // marker for controller method interception
    }

    @Around("controllerMethods()")
    public Object auditControllerActions(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        HttpServletRequest request = getCurrentRequest();
        String requestPath = request != null ? request.getRequestURI() : "unknown";
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";

        if (!shouldLogSecurityAction(requestPath, methodName, httpMethod)) {
            return joinPoint.proceed();
        }

        String username = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            username = authentication.getName();
        }

        try {
            Object result = joinPoint.proceed();
            if (username != null) {
                auditLogService.record("CONTROLLER_SECURITY_ACTION",
                        username,
                        null,
                        "Security-sensitive controller action: " + methodName + " at " + requestPath + " (" + httpMethod + ")");
            }
            return result;
        } catch (Throwable ex) {
            if (username != null) {
                auditLogService.record("CONTROLLER_SECURITY_ACTION_FAILED",
                        username,
                        null,
                        "Security-sensitive controller action failed: " + methodName + " at " + requestPath + " (" + httpMethod + ") - " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
            throw ex;
        }
    }

    private boolean shouldLogSecurityAction(String requestPath, String methodName, String httpMethod) {
        if (requestPath == null || "unknown".equals(requestPath)) {
            return false;
        }

        String normalizedPath = requestPath.toLowerCase();
        String normalizedMethod = methodName.toLowerCase();

        if ("get".equalsIgnoreCase(httpMethod) && !normalizedPath.contains("/admin/privileges")
                && !normalizedPath.contains("/admin/users")
                && !normalizedPath.contains("/profile/change-password")) {
            return false;
        }

        if (normalizedPath.contains("/admin/privileges")
                || normalizedPath.contains("/admin/users")
                || normalizedPath.contains("/profile/change-password")
                || normalizedPath.contains("/logout")) {
            return true;
        }

        return normalizedMethod.contains("grant")
                || normalizedMethod.contains("revoke")
                || normalizedMethod.contains("reset")
                || normalizedMethod.contains("toggle")
                || normalizedMethod.contains("change")
                || normalizedMethod.contains("create")
                || normalizedMethod.contains("delete")
                || normalizedMethod.contains("disable")
                || normalizedMethod.contains("enable");
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
