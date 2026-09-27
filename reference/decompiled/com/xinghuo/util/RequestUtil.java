/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.util.RequestUtil
 *  jakarta.servlet.http.HttpServletRequest
 */
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

