package com.xinghuo.util;

import jakarta.servlet.http.HttpServletRequest;

/** Shared request metadata extraction for public/security/audit code. */
public final class RequestUtil {
    private static final int MAX_IP_LENGTH = 45;

    private RequestUtil() {
    }

    /**
     * Returns the client IP using the same trusted proxy semantics as the account layer.
     *
     * <p>The production site runs behind nginx. nginx overwrites {@code X-Real-IP} with
     * {@code $remote_addr} and appends the observed source to {@code X-Forwarded-For}, so the last
     * XFF segment is the trusted fallback. Direct {@code remoteAddr} is used only when neither
     * proxy header provides a usable address.
     */
    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String real = request.getHeader("X-Real-IP");
        if (usableIp(real)) {
            return real.trim();
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            String last = parts[parts.length - 1].trim();
            if (usableIp(last)) {
                return last;
            }
        }

        String remote = request.getRemoteAddr();
        return usableIp(remote) ? remote.trim() : "unknown";
    }

    private static boolean usableIp(String value) {
        return value != null && !value.isBlank() && value.trim().length() <= MAX_IP_LENGTH;
    }
}
