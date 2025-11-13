package com.i2i.user_management.Exception;

/**
 * Exception thrown when an external integration (like currency API) fails.
 * Results in HTTP 503 (Service Unavailable).
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalServiceException(String message) {
        super(message);
    }

}
