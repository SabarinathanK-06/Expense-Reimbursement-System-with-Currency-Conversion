package com.i2i.user_management.util;

import com.i2i.user_management.Exception.BadRequestException;

import java.util.function.Consumer;

/**
 * Utility class for common validation operations across the application.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Ensures that the provided value is not null.
     *
     * @param value the string value to validate
     * @return the validated non-null string value
     * @throws BadRequestException if the value is null
     */
    public static String requestedNonNull(String value) {
        if (value == null) {
            throw new BadRequestException("Current user email is required");
        }
        return value;
    }

    public static <T> void updateIfNotNull(T value, Consumer<T> setter) {
        if (value != null && value != "") {
            setter.accept(value);
        }
    }

}
