package com.rainbow.springboot.thymeleafdemo.exception;

/** Thrown when trying to create a staff account with a username that already exists. */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
