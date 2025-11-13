package com.i2i.user_management.Exception;

/**
 * Thrown when a role assignment operation fails due to invalid or deleted role.
 */
public class RoleAssignmentException extends ApplicationException {

    public RoleAssignmentException(String message) {
        super(message);
    }
}
