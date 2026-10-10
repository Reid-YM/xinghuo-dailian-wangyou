package com.xinghuo.language.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LanguageController {
    private static final String COOKIE_NAME = "xh_lang";
    private static final int MAX_AGE_SECONDS = 31536000;

    @GetMapping(value={"/language"})
    String switchLanguage(@RequestParam(defaultValue="zh") String lang, @RequestParam(defaultValue="/") String returnTo, HttpServletResponse response) {
        String normalized = "en".equalsIgnoreCase(lang) ? "en" : "zh";
        Cookie cookie = new Cookie(COOKIE_NAME, normalized);
        cookie.setPath("/");
        cookie.setMaxAge(31536000);
        cookie.setHttpOnly(false);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return "redirect:" + this.targetFor(normalized, returnTo);
    }

    private String targetFor(String lang, String returnTo) {
        return this.safePath(returnTo);
    }

    private String safePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() || value.startsWith("//")) {
                return "/";
            }
        }
        catch (IllegalArgumentException ex) {
            return "/";
        }
        return value.startsWith("/") ? value : "/";
    }
}

