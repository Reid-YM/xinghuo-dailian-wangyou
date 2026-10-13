package com.xinghuo.account.service;

import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;

/** 请求信息提取。站点部署在 nginx 之后，直连地址永远是 127.0.0.1。 */
public final class RequestUtils {

    private static final int UA_MAX = 300;

    private RequestUtils() {
    }

    /**
     * 取客户端真实 IP。
     *
     * <p>复用全站统一的可信代理解析规则：优先 nginx 覆写的 X-Real-IP，其次取
     * X-Forwarded-For 最后一跳，最后回退 remoteAddr。账号侧保留既有 null 语义。
     */
    public static String clientIp(HttpServletRequest request) {
        return RequestUtil.clientIpOrNull(request);
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
