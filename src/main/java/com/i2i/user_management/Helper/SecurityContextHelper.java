package com.i2i.user_management.Helper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static String extractEmailFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName(); // username = email
    }
}

