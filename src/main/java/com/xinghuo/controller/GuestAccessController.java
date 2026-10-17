package com.xinghuo.controller;

import com.xinghuo.service.GuestQuoteFlowService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** One-time staff link -> HttpOnly browser capability -> stable guest business. */
@Controller
public class GuestAccessController {
    private static final Duration COOKIE_TTL = Duration.ofDays(90);

    private final GuestQuoteFlowService guests;

    public GuestAccessController(GuestQuoteFlowService guests) {
        this.guests = guests;
    }

    @GetMapping("/guest/access/{token}")
    public ResponseEntity<Void> exchange(
            @PathVariable String token,
            HttpServletRequest request) {
        GuestQuoteFlowService.ExchangeResult result = guests.exchangeAccessLink(
                token,
                cookie(request));
        boolean secure = request.isSecure()
                || String.valueOf(request.getHeader("X-Forwarded-Proto"))
                        .toLowerCase()
                        .startsWith("https");
        ResponseCookie guestCookie = ResponseCookie.from(
                        GuestQuoteFlowService.COOKIE_NAME,
                        result.session().token())
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_TTL)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, guestCookie.toString());
        headers.add("Referrer-Policy", "no-referrer");
        headers.setLocation(URI.create("/record/" + result.businessNo()));
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String cookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (GuestQuoteFlowService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
