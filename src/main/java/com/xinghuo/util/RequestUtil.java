package com.xinghuo.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtil {
    private RequestUtil() {
    }

    public static String clientIp(HttpServletRequest r) {
        String ip = r.getRemoteAddr();
        return ip == null || ip.isBlank() ? "unknown" : ip.trim();
    }
}

