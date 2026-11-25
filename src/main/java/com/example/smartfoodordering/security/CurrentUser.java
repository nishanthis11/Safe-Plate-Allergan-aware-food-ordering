package com.example.smartfoodordering.security;

import com.example.smartfoodordering.exception.AccessDeniedAppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Single place to resolve the authenticated principal; services never trust client-supplied user ids. */
public final class CurrentUser {
    private CurrentUser() {}

    public static CustomUserDetails get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            throw new AccessDeniedAppException("Authentication required");
        }
        return details;
    }

    public static Long id() { return get().getId(); }
    public static String email() { return get().getUsername(); }
}
