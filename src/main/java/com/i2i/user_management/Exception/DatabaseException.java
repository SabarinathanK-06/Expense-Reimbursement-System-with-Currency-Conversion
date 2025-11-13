package com.i2i.user_management.Exception;

public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }
}

