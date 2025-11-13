package com.i2i.user_management.Exception;

/**
 * Thrown when a user cannot be found in the system.
 */
public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
