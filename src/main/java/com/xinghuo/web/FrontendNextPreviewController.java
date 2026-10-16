package com.xinghuo.web;

import java.io.IOException;
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
    private static final String N13_RUNTIME = "<script src=\"/next/n13-record-detail.js\" defer></script>";
    private static final String N14_RUNTIME = "<script src=\"/next/n14-route-continuity.js\" defer></script>";
    private static final String N15_RUNTIME = "<script src=\"/next/n15-auth-return.js\" defer></script>";
    private static final String N16_RUNTIME = "<script src=\"/next/n16-account-deep-links.js\" defer></script>";
    private static final String N17_RUNTIME = "<script src=\"/next/n17-account-navigation.js\" defer></script>";
    private static final String N18_RUNTIME = "<script src=\"/next/n18-account-detail-deep-links.js\" defer></script>";
    private static final String N19_RUNTIME = "<script src=\"/next/n19-account-detail-context.js\" defer></script>";
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
        html = versionRuntimes(html);
        if (configuration != null) {
            var site = configuration.publicConfig();
            String original = request == null ? "/" : (String)request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
            String path = original == null ? request.getRequestURI().replaceFirst("^/next", "") : original;
            if (path.isBlank() || "/home".equals(path) || "/index.html".equals(path)) path = "/";
            String brand = site.brandName() == null ? "星火网游" : site.brandName();
            String title = path.startsWith("/account") ? "个人中心 · " + brand
                    : "/services".equals(path) ? "服务大厅 · " + brand
                    : path.startsWith("/service/") ? "服务详情 · " + brand
                    : path.startsWith("/record/") ? "业务记录 · " + brand
                    : "/quote".equals(path) ? "提交询价 · " + brand
                    : site.seoTitle() == null ? brand : site.seoTitle();
            String description = site.seoDescription() == null ? site.heroSubtitle() : site.seoDescription();
            html = html.replace("<title>星火网游服务平台</title>", "<title>" + HtmlUtils.htmlEscape(title) + "</title>");
            String metadata = "<meta name=\"description\" content=\"" + HtmlUtils.htmlEscape(description == null ? "" : description) + "\"/><link rel=\"canonical\" href=\"https://wydlgf.cn" + HtmlUtils.htmlEscape(path) + "\"/>";
            if (path.startsWith("/account") || path.startsWith("/record/")) metadata += "<meta name=\"robots\" content=\"noindex,nofollow\"/>";
            else if (path.startsWith("/service/")) metadata += "<meta name=\"robots\" content=\"noindex,follow\"/>";
            html = html.replace("</head>", metadata + "</head>");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header("X-Xinghuo-Frontend", "next")
                .cacheControl(CacheControl.noStore())
                .body(html);
    }
    private String versionRuntimes(String html) throws IOException {
        for (String name : new String[]{"next/shell.js","next/n2.js","next/n3.js","next/n4.js","next/n4-mini.js","next/n5.js","next/n5-actions.js","next/n6-account-security.js","next/n7-auth.js","next/n8-integration.js","next/n10-service-detail.js","next/n11-related-services.js","next/n12-quote-context.js","next/n13-record-detail.js","next/n14-route-continuity.js","next/n15-auth-return.js","next/n16-account-deep-links.js","next/n17-account-navigation.js","next/n18-account-detail-deep-links.js","next/n19-account-detail-context.js","assets/global-ui-controls.js","assets/frontend-controls-runtime.js","assets/site-image-controls.js","assets/site-image-controls.css"}) {
            String version = runtimeVersions.get(name);
            if (version == null) {
                try (var input = new ClassPathResource("static/" + name).getInputStream()) {
                    version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
                } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
                runtimeVersions.put(name, version);
            }
            String path = "/" + name;
            html = html.replace("src=\"" + path + "\"", "src=\"" + path + "?v=" + version + "\"");
            html = html.replace("href=\"" + path + "\"", "href=\"" + path + "?v=" + version + "\"");
        }
        return html;
    }
}
