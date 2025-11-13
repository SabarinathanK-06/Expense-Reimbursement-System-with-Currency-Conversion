package com.i2i.user_management.Exception;

/**
 * Thrown when authentication or login attempt fails.
 */
public class AuthenticationFailedException extends ApplicationException {

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
