package com.i2i.user_management.Exception;

/**
 * Exception thrown when a requested Role is not found or inactive.
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String message) {
        super(message);
    }

    public RoleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
