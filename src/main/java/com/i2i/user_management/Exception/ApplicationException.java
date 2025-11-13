package com.i2i.user_management.Exception;

/**
 * Base exception for all custom application-level exceptions.
 * Extends RuntimeException for transactional rollback and unchecked propagation.
 */
public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
