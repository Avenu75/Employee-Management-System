package com.rainbow.springboot.thymeleafdemo.exception;

/**
 * Thrown when an Employee cannot be found by id.
 * Handled centrally by {@link GlobalExceptionHandler}, which shows a friendly
 * error page instead of a raw stack trace.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
