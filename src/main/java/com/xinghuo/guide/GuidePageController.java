package com.xinghuo.guide;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GuidePageController {
    private final GuideContentService guides;
    private final ObjectMapper mapper;

    public GuidePageController(GuideContentService guides, ObjectMapper mapper) {
        this.guides = guides;
        this.mapper = mapper;
    }

    @GetMapping("/guides")
    String list(@RequestParam(required = false) String game, @RequestParam(required = false) String q, Model model) {
        List<GuideModels.GuideSummary> rows = guides.publicList(game, q, 200);
        model.addAttribute("guides", rows);
        model.addAttribute("guideCount", rows.size());
        model.addAttribute("activeGame", game == null ? "" : game);
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("canonicalUrl", "https://wydlgf.cn/guides");
        model.addAttribute("pageTitle", "多游戏代练攻略与任务路径 | 星火网游代练平台");
        model.addAttribute("pageDescription", "覆盖 28 款热门网游与主机游戏的服务攻略、推进顺序、账号安全说明和可下单商品推荐。");
        // 列表页的结构化数据在服务端生成：站点 CSP 是 script-src 'self'，
        // 页面里的内联脚本会被拦截，所以不能靠前端拼。
        model.addAttribute("listJsonLd", jsonLdSafe(listJsonLd(rows)));
        return "guides";
    }

    /** 攻略列表的 ItemList 结构化数据，让搜索引擎识别这是一个攻略合集。 */
    private String listJsonLd(List<GuideModels.GuideSummary> rows) {
        ArrayNode items = mapper.createArrayNode();
        int pos = 1;
        for (GuideModels.GuideSummary g : rows) {
            ObjectNode item = items.addObject();
            item.put("@type", "ListItem");
            item.put("position", pos++);
            item.put("name", g.title());
            item.put("url", absolute(g.canonicalUrl() == null ? "/guides/" + g.slug() : g.canonicalUrl()));
        }
        ObjectNode root = mapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "ItemList");
        root.put("name", "多游戏代练攻略与任务路径");
        root.put("numberOfItems", items.size());
        root.set("itemListElement", items);
        return root.toString();
    }

    @GetMapping("/guides/{slug}")
    String detail(@PathVariable String slug, Model model) {
        GuideModels.GuideDetail detail = guides.publicDetail(slug);
        GuideModels.GuideSummary guide = detail.guide();
        model.addAttribute("guide", guide);
        model.addAttribute("sections", detail.sections());
        model.addAttribute("products", detail.products());
        model.addAttribute("faqs", detail.faqs());
        model.addAttribute("relatedGuides", detail.relatedGuides());
        model.addAttribute("canonicalUrl", guide.canonicalUrl() == null || guide.canonicalUrl().isBlank() ? "https://wydlgf.cn/guides/" + guide.slug() : guide.canonicalUrl());
        model.addAttribute("pageTitle", guide.seoTitle() == null || guide.seoTitle().isBlank() ? guide.title() + " | 星火网游代练平台" : guide.seoTitle());
        model.addAttribute("pageDescription", guide.seoDescription() == null || guide.seoDescription().isBlank() ? guide.summary() : guide.seoDescription());
        model.addAttribute("ogImage", absolute(guide.coverImage()));
        model.addAttribute("articleJsonLd", jsonLdSafe(articleJsonLd(guide)));
        model.addAttribute("breadcrumbJsonLd", jsonLdSafe(breadcrumbJsonLd(guide)));
        model.addAttribute("faqJsonLd", jsonLdSafe(faqJsonLd(detail.faqs())));
        return "guide";
    }

    @GetMapping("/admin/guides")
    String admin(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "redirect:/admin/xingwei/guides";
    }

    private String articleJsonLd(GuideModels.GuideSummary guide) {
        ObjectNode root = mapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "Article");
        root.put("headline", guide.title());
        root.put("description", guide.summary());
        root.put("datePublished", guide.publishedAt() == null ? "" : guide.publishedAt().toString());
        root.put("dateModified", guide.updatedAt() == null ? "" : guide.updatedAt().toString());
        root.put("inLanguage", guide.language());
        ObjectNode author = root.putObject("author");
        author.put("@type", "Organization");
        author.put("name", guide.authorName());
        ObjectNode publisher = root.putObject("publisher");
        publisher.put("@type", "Organization");
        publisher.put("name", "星火网游代练平台");
        ObjectNode image = root.putObject("image");
        image.put("@type", "ImageObject");
        image.put("url", absolute(guide.coverImage()));
        root.put("mainEntityOfPage", absolute(guide.canonicalUrl() == null ? "/guides/" + guide.slug() : guide.canonicalUrl()));
        return root.toString();
    }

    private String breadcrumbJsonLd(GuideModels.GuideSummary guide) {
        ArrayNode items = mapper.createArrayNode();
        items.add(crumb(1, "首页", "https://wydlgf.cn/"));
        items.add(crumb(2, "攻略中心", "https://wydlgf.cn/guides"));
        items.add(crumb(3, guide.title(), absolute(guide.canonicalUrl() == null ? "/guides/" + guide.slug() : guide.canonicalUrl())));
        ObjectNode root = mapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.set("itemListElement", items);
        return root.toString();
    }

    private String faqJsonLd(List<GuideModels.GuideFaq> faqs) {
        if (faqs == null || faqs.isEmpty()) {
            return "{}";
        }
        ArrayNode items = mapper.createArrayNode();
        for (GuideModels.GuideFaq faq : faqs) {
            ObjectNode question = mapper.createObjectNode();
            question.put("@type", "Question");
            question.put("name", faq.question());
            ObjectNode answer = question.putObject("acceptedAnswer");
            answer.put("@type", "Answer");
            answer.put("text", faq.answer());
            items.add(question);
        }
        ObjectNode root = mapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "FAQPage");
        root.set("mainEntity", items);
        return root.toString();
    }

    private ObjectNode crumb(int position, String name, String url) {
        ObjectNode node = mapper.createObjectNode();
        node.put("@type", "ListItem");
        node.put("position", position);
        node.put("name", name);
        node.put("item", url);
        return node;
    }

    private String absolute(String value) {
        if (value == null || value.isBlank()) {
            return "https://wydlgf.cn/og.png";
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return value.startsWith("/") ? "https://wydlgf.cn" + value : "https://wydlgf.cn/" + value;
    }

    /**
     * JSON-LD 安全化。
     *
     * <p>这些值经 {@code th:utext} 不转义写入 {@code <script type="application/ld+json">}，
     * 而 Jackson 的 {@code toString()} 只转义引号、反斜杠与控制字符，<b>不转义 {@code <}</b>。
     * 只要标题、摘要或 FAQ 里出现 {@code </script>}，就能提前闭合脚本元素并注入 HTML。
     * CSP 会挡掉注入的内联脚本执行，但 {@code meta refresh} 跳转与样式注入仍然生效。
     *
     * <p>把三个 HTML 敏感字符换成 JSON 的 Unicode 转义（反斜杠 + u + 四位十六进制）：
     * JSON 语义完全不变，
     * 但内容再也无法闭合标签。
     */
    private static String jsonLdSafe(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        return json.replace("<", "\\u003c")
                   .replace(">", "\\u003e")
                   .replace("&", "\\u0026");
    }
}
