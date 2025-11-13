package com.i2i.user_management.Exception;

/**
 * Thrown when a user registration process fails.
 */
public class RegistrationException extends ApplicationException {

    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
