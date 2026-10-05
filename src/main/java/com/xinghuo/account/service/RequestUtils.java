package com.xinghuo.account.service;

import jakarta.servlet.http.HttpServletRequest;

/** 请求信息提取。站点部署在 nginx 之后，直连地址永远是 127.0.0.1。 */
public final class RequestUtils {

    private static final int UA_MAX = 300;

    private RequestUtils() {
    }

    /**
     * 取客户端真实 IP。
     *
     * <p>nginx 会设置 X-Forwarded-For，取第一段（最靠近客户端的那个）。
     * 注意：该头可被伪造，因此它只用于展示与限流分桶，绝不能当作鉴权依据。
     */
    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 优先 X-Real-IP：本站 nginx 用 $remote_addr 覆写它，客户端无法伪造。
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank() && real.trim().length() <= 45) {
            return real.trim();
        }
        // 其次取 X-Forwarded-For 的**最后一段**，不是第一段。
        //
        // nginx 用的是 $proxy_add_x_forwarded_for，语义是「追加真实来源」：
        //     X-Forwarded-For: <客户端伪造值>, <真实 IP>
        // 因此第一段完全由攻击者控制。曾经取第一段，导致限流分桶键可被随意切换
        // （伪造 XFF 即可绕过全站 IP 限流），且审计里的 IP 也被污染。
        // 最后一跳是 nginx 亲眼看到的地址，才是可信的那个。
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            String last = parts[parts.length - 1].trim();
            if (!last.isEmpty() && last.length() <= 45) {
                return last;
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null && remote.length() <= 45 ? remote : null;
    }

    public static String userAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        return ua.length() > UA_MAX ? ua.substring(0, UA_MAX) : ua;
    }

    /**
     * 由 UA 粗略推断设备标签，仅用于「我的设备」列表展示。
     * 不做精确识别，也不作为安全判断依据。
     */
    public static String deviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "未知设备";
        }
        String ua = userAgent.toLowerCase(java.util.Locale.ROOT);
        String os;
        if (ua.contains("iphone")) {
            os = "iPhone";
        } else if (ua.contains("ipad")) {
            os = "iPad";
        } else if (ua.contains("android")) {
            os = "Android";
        } else if (ua.contains("mac os")) {
            os = "macOS";
        } else if (ua.contains("windows")) {
            os = "Windows";
        } else if (ua.contains("linux")) {
            os = "Linux";
        } else {
            os = "未知系统";
        }
        String browser;
        if (ua.contains("edg/")) {
            browser = "Edge";
        } else if (ua.contains("chrome/")) {
            browser = "Chrome";
        } else if (ua.contains("safari/")) {
            browser = "Safari";
        } else if (ua.contains("firefox/")) {
            browser = "Firefox";
        } else {
            browser = "浏览器";
        }
        return os + " · " + browser;
    }
}
