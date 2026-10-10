package com.xinghuo.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.guide.application.GuideQueryModels;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuideQueryCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void queryProjectionKeepsPublicJsonShape() {
        Instant published = Instant.parse("2026-10-01T00:00:00Z");
        Instant updated = Instant.parse("2026-10-02T00:00:00Z");
        GuideQueryModels.GuideSummary summary = new GuideQueryModels.GuideSummary(
                1L, "CODMW4", "使命召唤", "mw4-guide", "攻略", "副标题", "摘要", "/cover.png",
                "BEGINNER", 30, "zh-CN", "星火攻略组", "官方", "https://example.com", "PUBLISHED",
                true, 10, List.of("武器", "活动"), "SEO", "SEO描述", "https://wydlgf.cn/guides/mw4-guide",
                12L, published, updated, 1, 1);
        GuideQueryModels.GuideDetail detail = new GuideQueryModels.GuideDetail(
                summary,
                List.of(new GuideQueryModels.GuideSection(2L, "route", "路线", "正文", null, "路线图", 10)),
                List.of(new GuideQueryModels.GuideProduct(
                        3L, "service", "服务", "EVENT", "描述", "FIXED", new BigDecimal("35.00"),
                        null, null, "60分钟", "/service.png", "推荐", "商品描述", true, 10)),
                List.of(new GuideQueryModels.GuideFaq(4L, "问题？", "回答。", 10)),
                List.of(new GuideQueryModels.GuideRelated(5L, "CODMW4", "使命召唤", "related", "相关攻略", "/related.png")));

        JsonNode summaryJson = mapper.valueToTree(summary);
        assertEquals(Set.of(
                "id", "gameCode", "gameName", "slug", "title", "subtitle", "summary", "coverImage",
                "difficulty", "estimatedMinutes", "language", "authorName", "sourceName", "sourceUrl",
                "status", "featured", "sortOrder", "tags", "seoTitle", "seoDescription", "canonicalUrl",
                "viewCount", "publishedAt", "updatedAt", "sectionCount", "productCount"), fields(summaryJson));
        assertEquals("CODMW4", summaryJson.get("gameCode").asText());
        assertEquals("mw4-guide", summaryJson.get("slug").asText());

        JsonNode detailJson = mapper.valueToTree(detail);
        assertEquals(Set.of("guide", "sections", "products", "faqs", "relatedGuides"), fields(detailJson));
        assertEquals(Set.of(
                "serviceId", "slug", "name", "category", "shortDesc", "priceMode", "priceFixed",
                "priceMin", "priceMax", "durationText", "imageUrl", "label", "description", "featured",
                "sortOrder"), fields(detailJson.get("products").get(0)));
    }

    private Set<String> fields(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
