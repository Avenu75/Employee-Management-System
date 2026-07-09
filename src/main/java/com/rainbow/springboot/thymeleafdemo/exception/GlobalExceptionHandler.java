package com.rainbow.springboot.thymeleafdemo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Centralised error handling for the whole application ("cross-cutting concern").
 *
 * Without this class, an unhandled exception (e.g. an employee id that does not
 * exist) would show Spring Boot's default ugly whitelabel error page/stack trace.
 * Instead, we log the error and render a friendly "error.html" page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmployeeNotFoundException.class)
    public String handleEmployeeNotFound(EmployeeNotFoundException ex, Model model) {
        log.error("Employee not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public String handleDepartmentNotFound(DepartmentNotFoundException ex, Model model) {
        log.error("Department not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFound(UserNotFoundException ex, Model model) {
        log.error("User not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public String handlePermissionNotFound(PermissionNotFoundException ex, Model model) {
        log.error("Permission not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public String handleDuplicateUsername(DuplicateUsernameException ex, Model model) {
        log.warn("Duplicate username attempt: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * Thrown by Spring Security when @PreAuthorize denies access (e.g. a user
     * without CAN_DELETE_EMPLOYEE tries to hit the delete endpoint directly).
     * Shown as a friendly "not allowed" page instead of a generic 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Access denied: {}", ex.getMessage());
        model.addAttribute("errorMessage", "You do not have permission to perform this action.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        model.addAttribute("errorMessage", "Something went wrong. Please try again or contact the administrator.");
        return "error";
    }
}
