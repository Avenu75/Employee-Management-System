package com.rainbow.springboot.thymeleafdemo.exception;

public class OtherWorkerNotFoundException extends RuntimeException {

    public OtherWorkerNotFoundException(String message) {
        super(message);
    }

    public OtherWorkerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
