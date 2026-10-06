package com.xinghuo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.service.SiteSettingService;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/customer-service-settings")
public class CustomerServiceSettingsController {
    private final SiteSettingService settings;
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;
    private final boolean defaultEnabled;
    private final String defaultUrl;
    private final String gameUrl;
    private final String orderUrl;
    private final String afterSalesUrl;
    private final String businessUrl;

    public CustomerServiceSettingsController(
            SiteSettingService settings,
            AuditLogRepository audits,
            ObjectMapper mapper,
            @Value("${app.wecom-customer-service.enabled:false}") boolean defaultEnabled,
            @Value("${app.wecom-customer-service.url:}") String defaultUrl,
            @Value("${app.wecom-customer-service.game-url:}") String gameUrl,
            @Value("${app.wecom-customer-service.order-url:}") String orderUrl,
            @Value("${app.wecom-customer-service.after-sales-url:}") String afterSalesUrl,
            @Value("${app.wecom-customer-service.business-url:}") String businessUrl) {
        this.settings = settings;
        this.audits = audits;
        this.mapper = mapper;
        this.defaultEnabled = defaultEnabled;
        this.defaultUrl = defaultUrl;
        this.gameUrl = gameUrl;
        this.orderUrl = orderUrl;
        this.afterSalesUrl = afterSalesUrl;
        this.businessUrl = businessUrl;
    }

    @GetMapping
    Map<String, String> get() {
        return settings.customerServiceSettings(defaults());
    }

    @PatchMapping
    Map<String, String> update(
            @RequestBody Map<String, Object> body,
            Authentication authentication,
            HttpServletRequest request) throws Exception {
        Map<String, String> before = settings.customerServiceSettings(defaults());
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(body == null ? Map.of() : body);
        String reason = cleanReason(values.remove("reason"));
        Map<String, String> after = settings.updateCustomerServiceSettings(values, defaults());

        audits.save(AuditLog.builder()
                .action("CUSTOMER_SERVICE_SETTINGS_UPDATE")
                .targetType("SITE_SETTINGS")
                .targetId("customer-service")
                .operatorName(authentication == null ? "admin" : authentication.getName())
                .beforeJson(mapper.writeValueAsString(before))
                .afterJson(mapper.writeValueAsString(after))
                .reason(reason)
                .ipAddress(RequestUtil.clientIp(request))
                .requestId(request.getHeader("X-Request-Id"))
                .createdAt(Instant.now())
                .build());
        return after;
    }

    private Map<String, String> defaults() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put("customerServiceEnabled", String.valueOf(defaultEnabled));
        out.put("customerServiceDefaultUrl", clean(defaultUrl));
        out.put("customerServiceGameUrl", clean(gameUrl));
        out.put("customerServiceOrderUrl", clean(orderUrl));
        out.put("customerServiceAfterSalesUrl", clean(afterSalesUrl));
        out.put("customerServiceBusinessUrl", clean(businessUrl));
        return out;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanReason(Object value) {
        String reason = value == null ? "后台客服中心配置" : String.valueOf(value).trim();
        if (reason.isBlank()) reason = "后台客服中心配置";
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
