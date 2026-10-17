package com.xinghuo.controller;

import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.GuestQuoteFlowService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Adds the anonymous-quote capability without changing the public quote response contract.
 * Registered users keep the existing account-owned quote path; only anonymous POST /api/quote
 * responses receive an HttpOnly guest session and a guest_business_access row.
 */
@ControllerAdvice
public class GuestQuoteResponseAdvice implements ResponseBodyAdvice<ApiDtos.QuoteResponse> {
    private static final Duration COOKIE_TTL = Duration.ofDays(90);

    private final GuestQuoteFlowService guests;
    private final AccountSessionService accountSessions;

    public GuestQuoteResponseAdvice(GuestQuoteFlowService guests, AccountSessionService accountSessions) {
        this.guests = guests;
        this.accountSessions = accountSessions;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiDtos.QuoteResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public ApiDtos.QuoteResponse beforeBodyWrite(
            ApiDtos.QuoteResponse body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body == null || !(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        HttpServletRequest req = servletRequest.getServletRequest();
        if (!"POST".equalsIgnoreCase(req.getMethod()) || !"/api/quote".equals(req.getRequestURI())) {
            return body;
        }
        if (accountSessions.current(req).isPresent()) {
            return body;
        }

        GuestQuoteFlowService.SessionHandle session = guests.ensureSession(cookie(req));
        guests.attachQuote(session.id(), body.id(), body.businessNo());
        boolean secure = req.isSecure()
                || String.valueOf(req.getHeader("X-Forwarded-Proto")).toLowerCase().startsWith("https");
        ResponseCookie cookie = ResponseCookie.from(GuestQuoteFlowService.COOKIE_NAME, session.token())
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_TTL)
                .build();
        response.getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
        return body;
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
