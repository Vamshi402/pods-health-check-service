package com.optum.me.exception;

public class GenericException extends RuntimeException {
    public GenericException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericException(String message) {
        super(message);
    }
}