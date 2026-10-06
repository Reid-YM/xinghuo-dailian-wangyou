package com.xinghuo.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 企业微信原生微信客服入口。
 *
 * <p>本站只负责记录站内入口并跳转到服务端配置的官方客服链接；聊天、图片、文件等消息
 * 始终由企业微信原生通道承载。前端不能传入目标 URL，从而避免开放重定向。
 */
@Controller
public class CustomerServiceController {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceController.class);
    private static final Set<String> ALLOWED_TOPICS = Set.of(
            "general", "game", "order", "after-sales", "business");
    private static final URI CONTACT_FALLBACK = URI.create("/contact?customerService=unavailable");

    private final boolean enabled;
    private final URI officialCustomerServiceUri;

    public CustomerServiceController(
            @Value("${app.wecom-customer-service.enabled:false}") boolean enabled,
            @Value("${app.wecom-customer-service.url:}") String configuredUrl) {
        this.enabled = enabled;
        this.officialCustomerServiceUri = parseOfficialCustomerServiceUri(configuredUrl);
    }

    @GetMapping("/customer-service/start")
    ResponseEntity<Void> start(
            @RequestParam(name = "topic", defaultValue = "general") String topic,
            HttpServletRequest request) {
        String normalizedTopic = normalizeTopic(topic);
        String sourcePath = sourcePath(request.getHeader("Referer"));

        if (!enabled || officialCustomerServiceUri == null) {
            log.warn("企业微信原生客服入口未启用或链接无效，topic={} source={}", normalizedTopic, sourcePath);
            return redirect(CONTACT_FALLBACK);
        }

        log.info("企业微信原生客服入口点击 topic={} source={}", normalizedTopic, sourcePath);
        return redirect(officialCustomerServiceUri);
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private static String normalizeTopic(String topic) {
        if (topic == null) {
            return "general";
        }
        String normalized = topic.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_TOPICS.contains(normalized) ? normalized : "general";
    }

    /**
     * 只接受企业微信官方 HTTPS 客服链接，避免配置错误把用户导向第三方站点。
     */
    private static URI parseOfficialCustomerServiceUri(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(configuredUrl.trim());
            boolean official = "https".equalsIgnoreCase(uri.getScheme())
                    && "work.weixin.qq.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/kfid/");
            return official ? uri : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 只记录来源页面路径，不记录查询参数、Cookie、IP 等访客敏感信息。
     */
    private static String sourcePath(String referrer) {
        if (referrer == null || referrer.isBlank()) {
            return "unknown";
        }
        try {
            String path = URI.create(referrer).getPath();
            if (path == null || path.isBlank()) {
                return "/";
            }
            return path.length() <= 160 ? path : path.substring(0, 160);
        } catch (IllegalArgumentException ex) {
            return "unknown";
        }
    }
}
