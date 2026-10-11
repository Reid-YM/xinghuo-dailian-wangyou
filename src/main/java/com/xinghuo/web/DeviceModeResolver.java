package com.xinghuo.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Resolves the storefront presentation mode without changing the public URL.
 * Phones use the mobile view while tablets and desktop browsers keep the desktop view.
 */
@Component
public class DeviceModeResolver {

    public enum DeviceMode {
        DESKTOP("desktop"),
        MOBILE("mobile");

        private final String key;

        DeviceMode(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public DeviceMode resolve(HttpServletRequest request) {
        if (request == null) {
            return DeviceMode.DESKTOP;
        }

        String clientHint = request.getHeader("Sec-CH-UA-Mobile");
        if ("?1".equals(clientHint)) {
            return DeviceMode.MOBILE;
        }

        String rawUserAgent = request.getHeader("User-Agent");
        if (rawUserAgent == null || rawUserAgent.isBlank()) {
            return DeviceMode.DESKTOP;
        }
        String ua = rawUserAgent.toLowerCase(Locale.ROOT);

        // The product treats tablets as desktop-class layouts.
        if (ua.contains("ipad") || (ua.contains("macintosh") && ua.contains("mobile"))) {
            return DeviceMode.DESKTOP;
        }
        if (ua.contains("iphone") || ua.contains("ipod") || ua.contains("windows phone")) {
            return DeviceMode.MOBILE;
        }
        if (ua.contains("android") && ua.contains("mobile")) {
            return DeviceMode.MOBILE;
        }
        if (ua.contains("harmonyos") && (ua.contains("mobile") || ua.contains("phone"))) {
            return DeviceMode.MOBILE;
        }
        return ua.contains("mobi") ? DeviceMode.MOBILE : DeviceMode.DESKTOP;
    }
}
