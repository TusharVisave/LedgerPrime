package com.expensetracker.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Builds a hardened auth cookie: always HttpOnly, Secure flag configurable
     * for local development, SameSite configurable (Strict/Lax).
     */
    public static ResponseCookie buildCookie(String name, String value, Duration maxAge,
                                             String path, boolean secure, String sameSite) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(normalizeSameSite(sameSite))
                .build();
    }

    public static ResponseCookie clearCookie(String name, String path,
                                             boolean secure, String sameSite) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(0)
                .sameSite(normalizeSameSite(sameSite))
                .build();
    }

    /**
     * Only Strict or Lax are accepted per policy; anything else (including
     * "None", which would also require Secure) falls back to Lax.
     */
    private static String normalizeSameSite(String sameSite) {
        if (sameSite != null && sameSite.trim().equalsIgnoreCase("Strict")) {
            return "Strict";
        }
        return "Lax";
    }
}
