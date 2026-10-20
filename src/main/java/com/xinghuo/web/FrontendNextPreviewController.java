package com.xinghuo.web;

import java.io.IOException;
import com.xinghuo.model.GameCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.RequestDispatcher;
import com.xinghuo.service.SiteConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.HtmlUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shared Frontend Next document for parallel URLs and canonical view forwarding.
 *
 * <p>FrontendMigrationConfig owns the canonical switch after original authentication.
 * Existing handlers remain available through explicit rollback; /next aliases share this document.</p>
 */
@Controller
public class FrontendNextPreviewController {
    private static final String N2_RUNTIME = "<script src=\"/next/n2.js\" defer></script>";
    private static final String N3_RUNTIME = "<script src=\"/next/n3.js\" defer></script>";
    private static final String N4_RUNTIME = "<script src=\"/next/n4.js\" defer></script>";
    private static final String N4_MINI_RUNTIME = "<script src=\"/next/n4-mini.js\" defer></script>";
    private static final String N5_RUNTIME = "<script src=\"/next/n5.js\" defer></script>";
    private static final String N5_ACTIONS_RUNTIME = "<script src=\"/next/n5-actions.js\" defer></script>";
    private static final String N6_RUNTIME = "<script src=\"/next/n6-account-security.js\" defer></script>";
    private static final String N7_RUNTIME = "<script src=\"/next/n7-auth.js\" defer></script>";
    private static final String N8_RUNTIME = "<script src=\"/next/n8-integration.js\" defer></script>";
    private static final String N10_RUNTIME = "<script src=\"/next/n10-service-detail.js\" defer></script>";
    private static final String N11_RUNTIME = "<script src=\"/next/n11-related-services.js\" defer></script>";
    private static final String N12_RUNTIME = "<script src=\"/next/n12-quote-context.js\" defer></script>";
    private static final String GUEST_BUSINESS_RUNTIME = "<script src=\"/assets/guest-business.js\" defer></script>";
    private static final String N13_RUNTIME = "<script src=\"/next/n13-record-detail.js\" defer></script>";
    private static final String N14_RUNTIME = "<script src=\"/next/n14-route-continuity.js\" defer></script>";
    private static final String N15_RUNTIME = "<script src=\"/next/n15-auth-return.js\" defer></script>";
    private static final String N16_RUNTIME = "<script src=\"/next/n16-account-deep-links.js\" defer></script>";
    private static final String N17_RUNTIME = "<script src=\"/next/n17-account-navigation.js\" defer></script>";
    private static final String N18_RUNTIME = "<script src=\"/next/n18-account-detail-deep-links.js\" defer></script>";
    private static final String N19_RUNTIME = "<script src=\"/next/n19-account-detail-context.js\" defer></script>";
    private static final String N20_RUNTIME = "<script src=\"/next/n20-account-list-view-state.js\" defer></script>";
    private static final String N21_RUNTIME = "<script src=\"/next/n21-account-personal-assets.js\" defer></script>";
    private static final String GUIDE_COMMUNITY_RUNTIME = "<script src=\"/assets/guide-community.js\" defer></script>";
    private static final String N22_RUNTIME = "<script src=\"/next/n22-guide-detail.js\" defer></script>";
    private static final String[] STAGED_RUNTIMES = {
            "n23-guides-center.js", "n24-mastery.js", "n25-submission-workbench.js",
            "n26-records-center.js", "n26-record-history-bridge.js", "n27-support-trust.js",
            "n28-legal-single-source.js", "game-detail.js"
    };
    private SiteConfigService configuration;
    @Autowired void configuration(SiteConfigService configuration) { this.configuration = configuration; }
    private final Map<String,String> runtimeVersions = new ConcurrentHashMap<>();
    private final ClassPathResource index = new ClassPathResource("static/next/index.html");

    @GetMapping({
            "/next",
            "/next/",
            "/next/index.html",
            "/next/home",
            "/next/services",
            "/next/service/{slug}",
            "/next/guides/{slug}",
            "/next/quote",
            "/next/record/{businessNo}",
            "/next/account",
            "/next/account/requests",
            "/next/account/requests/{businessNo}",
            "/next/account/quotes",
            "/next/account/demands",
            "/next/account/orders",
            "/next/account/orders/{businessNo}",
            "/next/account/order",
            "/next/account/order-list",
            "/next/account/progress",
            "/next/account/tools",
            "/next/account/tool-progress",
            "/next/account/favorites",
            "/next/account/submissions",
            "/next/account/settings",
            "/next/account/profile",
            "/next/account/settings/profile",
            "/next/account/contact",
            "/next/account/settings/contact",
            "/next/account/security",
            "/next/account/settings/security",
            "/next/account/devices",
            "/next/account/activity"
    })
    public ResponseEntity<String> frontendNext(HttpServletRequest request) throws IOException {
        return render(request);
    }

    public ResponseEntity<String> frontendNext() throws IOException { return render(null); }

    private ResponseEntity<String> render(HttpServletRequest request) throws IOException {
        String html;
        try (var input = index.getInputStream()) {
            html = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        }
        if (!html.contains(N2_RUNTIME)) {
            html = html.replace("</body>", N2_RUNTIME + "</body>");
        }
        if (!html.contains(N3_RUNTIME)) {
            html = html.replace("</body>", N3_RUNTIME + "</body>");
        }
        if (!html.contains(N4_RUNTIME)) {
            html = html.replace("</body>", N4_RUNTIME + "</body>");
        }
        if (!html.contains(N4_MINI_RUNTIME)) {
            html = html.replace("</body>", N4_MINI_RUNTIME + "</body>");
        }
        if (!html.contains(N5_RUNTIME)) {
            html = html.replace("</body>", N5_RUNTIME + "</body>");
        }
        if (!html.contains(N5_ACTIONS_RUNTIME)) {
            html = html.replace("</body>", N5_ACTIONS_RUNTIME + "</body>");
        }
        if (!html.contains(N6_RUNTIME)) {
            html = html.replace("</body>", N6_RUNTIME + "</body>");
        }
        if (!html.contains(N7_RUNTIME)) {
            html = html.replace("</body>", N7_RUNTIME + "</body>");
        }
        if (!html.contains(N8_RUNTIME)) {
            html = html.replace("</body>", N8_RUNTIME + "</body>");
        }
        if (!html.contains(N10_RUNTIME)) {
            html = html.replace("</body>", N10_RUNTIME + "</body>");
        }
        if (!html.contains(N11_RUNTIME)) {
            html = html.replace("</body>", N11_RUNTIME + "</body>");
        }
        if (!html.contains(N12_RUNTIME)) {
            html = html.replace("</body>", N12_RUNTIME + "</body>");
        }
        if (!html.contains(GUEST_BUSINESS_RUNTIME)) html = html.replace("</body>", GUEST_BUSINESS_RUNTIME + "</body>");
        if (!html.contains(N13_RUNTIME)) {
            html = html.replace("</body>", N13_RUNTIME + "</body>");
        }
        if (!html.contains(N14_RUNTIME)) {
            html = html.replace("</body>", N14_RUNTIME + "</body>");
        }
        if (!html.contains(N15_RUNTIME)) {
            html = html.replace("</body>", N15_RUNTIME + "</body>");
        }
        if (!html.contains(N16_RUNTIME)) {
            html = html.replace("</body>", N16_RUNTIME + "</body>");
        }
        if (!html.contains(N17_RUNTIME)) {
            html = html.replace("</body>", N17_RUNTIME + "</body>");
        }
        if (!html.contains(N18_RUNTIME)) {
            html = html.replace("</body>", N18_RUNTIME + "</body>");
        }
        if (!html.contains(N19_RUNTIME)) {
            html = html.replace("</body>", N19_RUNTIME + "</body>");
        }
        if (!html.contains(N20_RUNTIME)) {
            html = html.replace("</body>", N20_RUNTIME + "</body>");
        }
        if (!html.contains(N21_RUNTIME)) {
            html = html.replace("</body>", N21_RUNTIME + "</body>");
        }
        if (!html.contains(GUIDE_COMMUNITY_RUNTIME)) {
            html = html.replace("</body>", GUIDE_COMMUNITY_RUNTIME + "</body>");
        }
        if (!html.contains(N22_RUNTIME)) {
            html = html.replace("</body>", N22_RUNTIME + "</body>");
        }
        // The loader's own digest cannot invalidate a changed, independently loaded resource.
        StringBuilder manifest = new StringBuilder("{");
        for (String name : STAGED_RUNTIMES) {
            if (manifest.length() > 1) manifest.append(',');
            manifest.append('"').append("/next/").append(name).append("\":\"")
                    .append(runtimeVersion("next/" + name)).append('"');
        }
        manifest.append('}');
        html = html.replace(N14_RUNTIME, N14_RUNTIME.replace(" defer", " data-stage-versions=\""
                + HtmlUtils.htmlEscape(manifest.toString()) + "\" defer"));
        html = versionRuntimes(html);
        Map<?, ?> originalMetadata = request != null && request.getAttribute("frontendOriginalMetadata") instanceof Map<?, ?> model ? model : Map.of();
        GameCatalog game = request == null ? null : (GameCatalog) request.getAttribute("frontendGame");
        String original = request == null ? null : (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        boolean previewRequest = original == null || original.equals("/next") || original.startsWith("/next/");
        String requestPath = request == null ? "/next" : request.getRequestURI();
        String path = original == null ? requestPath.replaceFirst("^/next(?=/|$)", "") : original;
        if (path.isBlank() || "/home".equals(path) || "/index.html".equals(path)) path = "/";
        boolean privatePage = path.startsWith("/account") || path.equals("/records") || path.startsWith("/record/");
        String robotsPolicy = privatePage ? "noindex,nofollow" : "noindex,follow";
        {
            var site = configuration == null ? null : configuration.publicConfig();
            String brand = site == null || site.brandName() == null ? "星火网游" : site.brandName();
            String title = path.startsWith("/account") ? "个人中心 · " + brand
                    : "/services".equals(path) ? "服务大厅 · " + brand
                    : path.startsWith("/service/") ? "服务详情 · " + brand
                    : path.startsWith("/guides/") ? "攻略详情 · " + brand
                    : path.startsWith("/record/") ? "业务记录 · " + brand
                    : "/quote".equals(path) ? "提交询价 · " + brand
                    : "/guides".equals(path) ? "攻略中心 · " + brand
                    : "/mastery".equals(path) ? "MW4 工具站 · " + brand
                    : "/records".equals(path) ? "业务记录 · " + brand
                    : "/contact".equals(path) ? "联系客服 · " + brand
                    : "/trust".equals(path) ? "服务保障 · " + brand
                    : "/rules".equals(path) ? "服务规则 · " + brand
                    : "/privacy".equals(path) ? "隐私说明 · " + brand
                    : "/rules-en".equals(path) ? "Service Rules · " + brand
                    : "/privacy-en".equals(path) ? "Privacy Policy · " + brand
                    : site == null || site.seoTitle() == null ? brand : site.seoTitle();
            String description = site == null ? "" : site.seoDescription() == null ? site.heroSubtitle() : site.seoDescription();
            if (game != null) {
                title = game.getSeoTitle() == null || game.getSeoTitle().isBlank() ? game.getName() + " 任务服务 · " + brand : game.getSeoTitle();
                description = game.getSeoDescription() == null || game.getSeoDescription().isBlank() ? game.getDescription() : game.getSeoDescription();
            }
            title = modelText(originalMetadata, "serviceTitle", modelText(originalMetadata, "pageTitle", title));
            description = modelText(originalMetadata, "serviceDescription", modelText(originalMetadata, "pageDescription", description));
            String canonicalUrl = modelText(originalMetadata, "canonicalUrl", "https://wydlgf.cn" + path);
            html = html.replace("<title>星火网游服务平台</title>", "<title>" + HtmlUtils.htmlEscape(title) + "</title>");
            String metadata = "<meta name=\"description\" content=\"" + HtmlUtils.htmlEscape(description == null ? "" : description) + "\"/><link rel=\"canonical\" href=\"" + HtmlUtils.htmlEscape(canonicalUrl) + "\"/>";
            if (previewRequest || privatePage) metadata += "<meta name=\"robots\" content=\"" + robotsPolicy + "\"/>";
            if (game != null) {
                String canonical = "https://wydlgf.cn" + path;
                String cover = game.getCoverImage();
                String image = cover != null && (cover.startsWith("https://") || cover.startsWith("/") && !cover.startsWith("//"))
                        ? cover.startsWith("/") ? "https://wydlgf.cn" + cover : cover : "https://wydlgf.cn/og.png";
                metadata += "<meta property=\"og:type\" content=\"website\"/>"
                        + "<meta property=\"og:title\" content=\"" + HtmlUtils.htmlEscape(title) + "\"/>"
                        + "<meta property=\"og:description\" content=\"" + HtmlUtils.htmlEscape(description == null ? "" : description) + "\"/>"
                        + "<meta property=\"og:url\" content=\"" + HtmlUtils.htmlEscape(canonical) + "\"/>"
                        + "<meta property=\"og:image\" content=\"" + HtmlUtils.htmlEscape(image) + "\"/>";
                var crumbs = java.util.List.of(
                        Map.of("@type", "ListItem", "position", 1, "name", "首页", "item", "https://wydlgf.cn/"),
                        Map.of("@type", "ListItem", "position", 2, "name", "服务大厅", "item", "https://wydlgf.cn/services"),
                        Map.of("@type", "ListItem", "position", 3, "name", game.getName(), "item", canonical));
                String json = new ObjectMapper().writeValueAsString(Map.of("@context", "https://schema.org", "@type", "BreadcrumbList", "itemListElement", crumbs))
                        .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
                metadata += "<script type=\"application/ld+json\">" + json + "</script>";
            }
            if (game == null && !originalMetadata.isEmpty()) {
                String image = modelText(originalMetadata, "ogImage", "https://wydlgf.cn/og.png");
                metadata += "<meta property=\"og:title\" content=\"" + HtmlUtils.htmlEscape(title) + "\"/>"
                        + "<meta property=\"og:description\" content=\"" + HtmlUtils.htmlEscape(description == null ? "" : description) + "\"/>"
                        + "<meta property=\"og:url\" content=\"" + HtmlUtils.htmlEscape(canonicalUrl) + "\"/>"
                        + "<meta property=\"og:image\" content=\"" + HtmlUtils.htmlEscape(image) + "\"/>";
                for (String key : new String[]{"productJsonLd", "articleJsonLd", "breadcrumbJsonLd", "faqJsonLd", "listJsonLd"}) {
                    String value = modelText(originalMetadata, key, "");
                    if (value.isBlank()) continue;
                    String json = new ObjectMapper().writeValueAsString(new ObjectMapper().readTree(value))
                            .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
                    metadata += "<script type=\"application/ld+json\">" + json + "</script>";
                }
            }
            html = html.replace("</head>", metadata + "</head>");
        }
        if (path.equals("/rules-en") || path.equals("/privacy-en")) html = html.replace("<html lang=\"zh-CN\">", "<html lang=\"en\">");
        var response = ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .header("X-Xinghuo-Frontend", "next")
                .cacheControl(CacheControl.noStore());
        if (previewRequest || privatePage) response.header("X-Robots-Tag", robotsPolicy);
        return response.body(html);
    }
    private static String modelText(Map<?, ?> model, String key, String fallback) {
        Object value = model.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }
    private String versionRuntimes(String html) throws IOException {
        for (String name : new String[]{"next/shell.js","next/n2.js","next/n3.js","next/n4.js","next/n4-mini.js","next/n5.js","next/n5-actions.js","next/n6-account-security.js","next/n7-auth.js","next/n8-integration.js","next/n10-service-detail.js","next/n11-related-services.js","next/n12-quote-context.js","next/n13-record-detail.js","next/n14-route-continuity.js","next/n15-auth-return.js","next/n16-account-deep-links.js","next/n17-account-navigation.js","next/n18-account-detail-deep-links.js","next/n19-account-detail-context.js","next/n20-account-list-view-state.js","next/n21-account-personal-assets.js","next/n22-guide-detail.js","assets/mw4-camo-images.js","assets/guest-business.js","assets/guide-community.js","assets/guide-community.css","assets/global-ui-controls.js","assets/frontend-controls-runtime.js","assets/site-image-controls.js","assets/site-image-controls.css"}) {
            String version = runtimeVersion(name);
            String path = "/" + name;
            html = html.replace("src=\"" + path + "\"", "src=\"" + path + "?v=" + version + "\"");
            html = html.replace("href=\"" + path + "\"", "href=\"" + path + "?v=" + version + "\"");
        }
        return html;
    }
    private String runtimeVersion(String name) throws IOException {
        String version = runtimeVersions.get(name);
        if (version == null) {
            try (var input = new ClassPathResource("static/" + name).getInputStream()) {
                version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
            } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
            runtimeVersions.put(name, version);
        }
        return version;
    }

}
