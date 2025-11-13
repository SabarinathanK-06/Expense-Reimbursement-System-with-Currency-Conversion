package com.i2i.user_management.Exception;

/**
 * Exception thrown when user input validation fails.
 * Results in HTTP 400 (Bad Request).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

}
