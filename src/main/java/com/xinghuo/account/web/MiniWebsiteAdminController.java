package com.xinghuo.account.web;

import com.xinghuo.account.service.MiniWebsiteAdminBridgeService;
import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.dto.ApiDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Mini routes are permitAll at SecurityConfig but every call first verifies
 * a fresh one-time wx.login code, a browser-confirmed binding, and current website RBAC.
 */
@RestController
@RequestMapping("/api/account/wechat-mini/admin")
public class MiniWebsiteAdminController {
    private final MiniWebsiteAdminBridgeService bridge;
    private final RateLimiter limiter;
    public MiniWebsiteAdminController(MiniWebsiteAdminBridgeService bridge, RateLimiter limiter) {
        this.bridge = bridge;
        this.limiter = limiter;
    }
    public record Code(@NotBlank String loginCode) {}
    public record Change(@NotBlank String loginCode, @NotBlank String orderNo,
                         @Valid ApiDtos.UpdateOrderRequest order) {}

    @PostMapping("/challenge")
    public MiniWebsiteAdminBridgeService.Challenge challenge(@Valid @RequestBody Code body,
                                                               HttpServletRequest request) {
        limiter.check("mini:admin:challenge:" + RequestUtils.clientIp(request),
                5, Duration.ofMinutes(15));
        return bridge.challenge(body.loginCode());
    }
    @PostMapping("/me")
    public MiniWebsiteAdminBridgeService.MiniAdminView me(@Valid @RequestBody Code body,
                                                           HttpServletRequest request) {
        limiter.check("mini:admin:me:" + RequestUtils.clientIp(request), 35, Duration.ofMinutes(15));
        return bridge.me(body.loginCode());
    }
    public record QuoteChange(@NotBlank String loginCode, @NotBlank String id,
                              @NotBlank String status, @NotBlank String reason) {}

    @PostMapping("/quotes")
    public List<MiniWebsiteAdminBridgeService.MiniQuoteView> quotes(@Valid @RequestBody Code body,
                                                                     HttpServletRequest request) {
        limiter.check("mini:admin:quotes:" + RequestUtils.clientIp(request), 30, Duration.ofMinutes(15));
        return bridge.listQuotes(body.loginCode());
    }

    @PostMapping("/quotes/update")
    public MiniWebsiteAdminBridgeService.MiniQuoteView updateQuote(
            @Valid @RequestBody QuoteChange body, HttpServletRequest request) {
        limiter.check("mini:admin:quotes:update:" + RequestUtils.clientIp(request),
                10, Duration.ofMinutes(15));
        java.util.UUID id;
        try {
            id = java.util.UUID.fromString(body.id());
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "询价标识格式无效");
        }
        return bridge.updateQuote(body.loginCode(), id, body.status(), body.reason(), request);
    }

    public record ConvertInput(@NotBlank String loginCode, @NotBlank String id,
                               @NotBlank String reason) {}

    @PostMapping("/quotes/convert")
    public MiniWebsiteAdminBridgeService.MiniOrderView convertQuote(
            @Valid @RequestBody ConvertInput body, HttpServletRequest request) {
        limiter.check("mini:admin:quotes:convert:" + RequestUtils.clientIp(request),
                6, Duration.ofMinutes(15));
        java.util.UUID id;
        try {
            id = java.util.UUID.fromString(body.id());
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "询价标识格式无效");
        }
        return bridge.convertQuote(body.loginCode(), id, body.reason(), request);
    }

    @PostMapping("/audit")
    public List<MiniWebsiteAdminBridgeService.MiniAuditView> audit(
            @Valid @RequestBody Code body, HttpServletRequest request) {
        limiter.check("mini:admin:audit:" + RequestUtils.clientIp(request),
                24, Duration.ofMinutes(15));
        return bridge.listAudit(body.loginCode());
    }

    @PostMapping("/orders")
    public List<MiniWebsiteAdminBridgeService.MiniOrderView> orders(@Valid @RequestBody Code body,
                                                                     HttpServletRequest request) {
        limiter.check("mini:admin:orders:" + RequestUtils.clientIp(request),
                35, Duration.ofMinutes(15));
        return bridge.listOrders(body.loginCode());
    }
    @PostMapping("/orders/update")
    public MiniWebsiteAdminBridgeService.MiniOrderView update(@Valid @RequestBody Change body,
                                                               HttpServletRequest request) {
        limiter.check("mini:admin:update:" + RequestUtils.clientIp(request),
                12, Duration.ofMinutes(15));
        return bridge.update(body.loginCode(), body.orderNo(), body.order(), request);
    }
}