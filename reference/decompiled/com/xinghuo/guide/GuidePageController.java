/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.node.ArrayNode
 *  com.fasterxml.jackson.databind.node.ObjectNode
 *  com.xinghuo.guide.GuideContentService
 *  com.xinghuo.guide.GuideModels$GuideDetail
 *  com.xinghuo.guide.GuideModels$GuideFaq
 *  com.xinghuo.guide.GuideModels$GuideSummary
 *  com.xinghuo.guide.GuidePageController
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.web.csrf.CsrfToken
 *  org.springframework.stereotype.Controller
 *  org.springframework.ui.Model
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestParam
 */
package com.xinghuo.guide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xinghuo.guide.GuideContentService;
import com.xinghuo.guide.GuideModels;
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

    @GetMapping(value={"/guides"})
    String list(@RequestParam(required=false) String game, @RequestParam(required=false) String q, Model model) {
        List rows = this.guides.publicList(game, q, 200);
        model.addAttribute("guides", (Object)rows);
        model.addAttribute("guideCount", (Object)rows.size());
        model.addAttribute("activeGame", (Object)(game == null ? "" : game));
        model.addAttribute("query", (Object)(q == null ? "" : q));
        model.addAttribute("canonicalUrl", (Object)"https://wydlgf.cn/guides");
        model.addAttribute("pageTitle", (Object)"\u591a\u6e38\u620f\u4ee3\u7ec3\u653b\u7565\u4e0e\u4efb\u52a1\u8def\u5f84 | \u661f\u706b\u7f51\u6e38\u4ee3\u7ec3\u5e73\u53f0");
        model.addAttribute("pageDescription", (Object)"\u8986\u76d6 28 \u6b3e\u70ed\u95e8\u7f51\u6e38\u4e0e\u4e3b\u673a\u6e38\u620f\u7684\u670d\u52a1\u653b\u7565\u3001\u63a8\u8fdb\u987a\u5e8f\u3001\u8d26\u53f7\u5b89\u5168\u8bf4\u660e\u548c\u53ef\u4e0b\u5355\u5546\u54c1\u63a8\u8350\u3002");
        return "guides";
    }

    @GetMapping(value={"/guides/{slug}"})
    String detail(@PathVariable String slug, Model model) {
        GuideModels.GuideDetail detail = this.guides.publicDetail(slug);
        GuideModels.GuideSummary guide = detail.guide();
        model.addAttribute("guide", (Object)guide);
        model.addAttribute("sections", (Object)detail.sections());
        model.addAttribute("products", (Object)detail.products());
        model.addAttribute("faqs", (Object)detail.faqs());
        model.addAttribute("relatedGuides", (Object)detail.relatedGuides());
        model.addAttribute("canonicalUrl", guide.canonicalUrl() == null || guide.canonicalUrl().isBlank() ? "https://wydlgf.cn/guides/" + guide.slug() : guide.canonicalUrl());
        model.addAttribute("pageTitle", guide.seoTitle() == null || guide.seoTitle().isBlank() ? guide.title() + " | \u661f\u706b\u7f51\u6e38\u4ee3\u7ec3\u5e73\u53f0" : guide.seoTitle());
        model.addAttribute("pageDescription", (Object)(guide.seoDescription() == null || guide.seoDescription().isBlank() ? guide.summary() : guide.seoDescription()));
        model.addAttribute("ogImage", (Object)this.absolute(guide.coverImage()));
        model.addAttribute("articleJsonLd", (Object)this.articleJsonLd(guide));
        model.addAttribute("breadcrumbJsonLd", (Object)this.breadcrumbJsonLd(guide));
        model.addAttribute("faqJsonLd", (Object)this.faqJsonLd(detail.faqs()));
        return "guide";
    }

    @GetMapping(value={"/admin/guides"})
    String admin(Model model, CsrfToken csrf, Authentication auth) {
        model.addAttribute("csrf", (Object)csrf);
        model.addAttribute("username", (Object)(auth == null ? "admin" : auth.getName()));
        return "admin-guides";
    }

    private String articleJsonLd(GuideModels.GuideSummary guide) {
        ObjectNode root = this.mapper.createObjectNode();
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
        publisher.put("name", "\u661f\u706b\u7f51\u6e38\u4ee3\u7ec3\u5e73\u53f0");
        ObjectNode image = root.putObject("image");
        image.put("@type", "ImageObject");
        image.put("url", this.absolute(guide.coverImage()));
        root.put("mainEntityOfPage", this.absolute((String)(guide.canonicalUrl() == null ? "/guides/" + guide.slug() : guide.canonicalUrl())));
        return root.toString();
    }

    private String breadcrumbJsonLd(GuideModels.GuideSummary guide) {
        ArrayNode items = this.mapper.createArrayNode();
        items.add((JsonNode)this.crumb(1, "\u9996\u9875", "https://wydlgf.cn/"));
        items.add((JsonNode)this.crumb(2, "\u653b\u7565\u4e2d\u5fc3", "https://wydlgf.cn/guides"));
        items.add((JsonNode)this.crumb(3, guide.title(), this.absolute((String)(guide.canonicalUrl() == null ? "/guides/" + guide.slug() : guide.canonicalUrl()))));
        ObjectNode root = this.mapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.set("itemListElement", (JsonNode)items);
        return root.toString();
    }

    private String faqJsonLd(List<GuideModels.GuideFaq> faqs) {
        if (faqs == null || faqs.isEmpty()) {
            return "{}";
        }
        ArrayNode items = this.mapper.createArrayNode();
        for (GuideModels.GuideFaq faq : faqs) {
            ObjectNode question = this.mapper.createObjectNode();
            question.put("@type", "Question");
            question.put("name", faq.question());
            ObjectNode answer = question.putObject("acceptedAnswer");
            answer.put("@type", "Answer");
            answer.put("text", faq.answer());
            items.add((JsonNode)question);
        }
        ObjectNode root = this.mapper.createObjectNode();
        root.put("@context", "https://schema.org");
        root.put("@type", "FAQPage");
        root.set("mainEntity", (JsonNode)items);
        return root.toString();
    }

    private ObjectNode crumb(int position, String name, String url) {
        ObjectNode node = this.mapper.createObjectNode();
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
}

