package com.xinghuo.controller;

import com.xinghuo.service.SiteSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 在线客服入口。
 *
 * <p>用户侧只暴露游戏咨询、订单咨询、售后处理、商务合作四类专属页面。
 * 真正进入会话时，再跳转到服务端为该类型配置的企业微信官方客服链接。
 * 每个咨询类型可配置独立客服链接；未配置时回退到默认客服链接。前端不能传入目标 URL，避免开放重定向。
 * 后台保存的 site_settings 优先于环境变量，因此管理员修改后无需重启即可生效。
 */
@Controller
public class CustomerServiceController {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceController.class);
    private static final Set<String> ALLOWED_TOPICS = Set.of(
            "general", "game", "order", "after-sales", "business");
    private static final URI CONTACT_FALLBACK = URI.create("/contact?customerService=unavailable");

    private static final Map<String, SupportTopic> SUPPORT_TOPICS = Map.of(
            "general", new SupportTopic(
                    "general",
                    "💬",
                    "在线咨询",
                    "不确定该选哪一类？先从这里开始，客服会根据你的情况继续确认。",
                    "把你想解决的问题直接说清楚即可。",
                    List.of("说明你想咨询的游戏或业务", "有业务编号时一并提供", "涉及异常时可准备截图"),
                    "开始在线咨询"),
            "game", new SupportTopic(
                    "game",
                    "🎮",
                    "游戏咨询",
                    "适合咨询服务内容、价格、平台支持、完成时间和当前活动需求。",
                    "信息越完整，客服越容易一次给出可执行方案。",
                    List.of("游戏名称与平台", "当前进度或目标内容", "希望完成的大致时间"),
                    "开始游戏咨询"),
            "order", new SupportTopic(
                    "order",
                    "📦",
                    "订单咨询",
                    "适合查询已提交业务的进度、交付状态，或补充新的订单要求。",
                    "优先准备业务编号，客服可以更快定位记录。",
                    List.of("业务编号或下单记录", "需要确认的进度节点", "如有补充要求请一次说明"),
                    "查询订单进度"),
            "after-sales", new SupportTopic(
                    "after-sales",
                    "🛠",
                    "售后处理",
                    "遇到异常、交付疑问、结果不一致或需要人工复核时从这里进入。",
                    "先说明现象，再补充截图；不要发送密码、验证码或令牌。",
                    List.of("业务编号", "问题发生的时间与现象", "能说明问题的截图或文件"),
                    "联系售后客服"),
            "business", new SupportTopic(
                    "business",
                    "🤝",
                    "商务合作",
                    "适合平台、渠道、团队、供应商或长期业务合作沟通。",
                    "先说明合作方向和资源情况，便于快速判断对接方式。",
                    List.of("合作类型或平台名称", "团队/渠道基本情况", "希望对接的合作方式"),
                    "开始商务沟通"));

    private final boolean configuredEnabled;
    private final String configuredDefaultUrl;
    private final Map<String, String> configuredTopicUrls;
    private final SiteSettingService siteSettings;

    public CustomerServiceController(
            @Value("${app.wecom-customer-service.enabled:false}") boolean enabled,
            @Value("${app.wecom-customer-service.url:}") String configuredUrl,
            @Value("${app.wecom-customer-service.game-url:}") String gameUrl,
            @Value("${app.wecom-customer-service.order-url:}") String orderUrl,
            @Value("${app.wecom-customer-service.after-sales-url:}") String afterSalesUrl,
            @Value("${app.wecom-customer-service.business-url:}") String businessUrl,
            SiteSettingService siteSettings) {
        this.configuredEnabled = enabled;
        this.configuredDefaultUrl = clean(configuredUrl);
        this.configuredTopicUrls = new LinkedHashMap<>();
        this.configuredTopicUrls.put("game", clean(gameUrl));
        this.configuredTopicUrls.put("order", clean(orderUrl));
        this.configuredTopicUrls.put("after-sales", clean(afterSalesUrl));
        this.configuredTopicUrls.put("business", clean(businessUrl));
        this.siteSettings = siteSettings;
    }

    /** /support 不再承载通用咨询页，统一回到四类客服中心。 */
    @GetMapping("/support")
    public String supportHome() {
        return "redirect:/contact";
    }

    @GetMapping("/support/{topic}")
    public String support(@PathVariable String topic, Model model) {
        String normalizedTopic = normalizeTopic(topic);
        if ("general".equals(normalizedTopic)) return "redirect:/contact";
        SupportTopic supportTopic = SUPPORT_TOPICS.get(normalizedTopic);
        model.addAttribute("supportTopic", supportTopic);
        model.addAttribute("supportTopics", List.of(
                SUPPORT_TOPICS.get("game"),
                SUPPORT_TOPICS.get("order"),
                SUPPORT_TOPICS.get("after-sales"),
                SUPPORT_TOPICS.get("business")));
        return "customer-service";
    }

    /** 兼容旧链接。新的前端使用 /customer-service/start/{topic}。 */
    @GetMapping("/customer-service/start")
    ResponseEntity<Void> start(
            @RequestParam(name = "topic", defaultValue = "general") String topic,
            HttpServletRequest request) {
        return redirectToCustomerService(topic, request);
    }

    @GetMapping("/customer-service/start/{topic}")
    ResponseEntity<Void> startPath(@PathVariable String topic, HttpServletRequest request) {
        return redirectToCustomerService(topic, request);
    }

    private ResponseEntity<Void> redirectToCustomerService(String topic, HttpServletRequest request) {
        String normalizedTopic = normalizeTopic(topic);
        String sourcePath = sourcePath(request.getHeader("Referer"));
        URI target = resolveCustomerServiceUri(normalizedTopic);

        if (!isEnabled() || target == null) {
            log.warn("在线客服入口未启用或链接无效，topic={} source={}", normalizedTopic, sourcePath);
            return redirect(CONTACT_FALLBACK);
        }

        log.info("在线客服入口点击 topic={} source={}", normalizedTopic, sourcePath);
        return redirect(target);
    }

    private boolean isEnabled() {
        if (siteSettings == null) return configuredEnabled;
        String value = siteSettings.get("customerServiceEnabled", String.valueOf(configuredEnabled));
        return Boolean.parseBoolean(value);
    }

    private URI resolveCustomerServiceUri(String topic) {
        String topicFallback = configuredTopicUrls.getOrDefault(topic, "");
        String topicKey = switch (topic) {
            case "game" -> "customerServiceGameUrl";
            case "order" -> "customerServiceOrderUrl";
            case "after-sales" -> "customerServiceAfterSalesUrl";
            case "business" -> "customerServiceBusinessUrl";
            default -> null;
        };

        if (topicKey != null) {
            String configured = siteSettings == null ? topicFallback : siteSettings.get(topicKey, topicFallback);
            URI topicUri = parseOfficialCustomerServiceUri(configured);
            if (topicUri != null) return topicUri;
        }

        String defaultUrl = siteSettings == null
                ? configuredDefaultUrl
                : siteSettings.get("customerServiceDefaultUrl", configuredDefaultUrl);
        return parseOfficialCustomerServiceUri(defaultUrl);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private static String normalizeTopic(String topic) {
        if (topic == null) return "general";
        String normalized = topic.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_TOPICS.contains(normalized) ? normalized : "general";
    }

    /** 只接受企业微信官方 HTTPS 客服链接，避免配置错误把用户导向第三方站点。 */
    static URI parseOfficialCustomerServiceUri(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) return null;
        try {
            URI uri = URI.create(configuredUrl.trim());
            String path = uri.getPath();
            boolean official = "https".equalsIgnoreCase(uri.getScheme())
                    && "work.weixin.qq.com".equalsIgnoreCase(uri.getHost())
                    && path != null
                    && (path.startsWith("/kfid/") || path.startsWith("/kf/"));
            return official ? uri : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** 只记录来源页面路径，不记录查询参数、Cookie、IP 等访客敏感信息。 */
    private static String sourcePath(String referrer) {
        if (referrer == null || referrer.isBlank()) return "unknown";
        try {
            String path = URI.create(referrer).getPath();
            if (path == null || path.isBlank()) return "/";
            return path.length() <= 160 ? path : path.substring(0, 160);
        } catch (IllegalArgumentException ex) {
            return "unknown";
        }
    }

    public record SupportTopic(
            String slug,
            String icon,
            String title,
            String description,
            String tip,
            List<String> checklist,
            String actionLabel) {}
}
