package com.i2i.user_management.Exception;

/**
 * Thrown when input data fails validation checks.
 */
public class ValidationException extends ApplicationException {

    public ValidationException(String message) {
        super(message);
    }
}
