package com.xinghuo.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.guide.application.GuideQueryModels;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuideQueryCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void queryProjectionKeepsLegacyJsonShape() {
        Instant published = Instant.parse("2026-10-01T00:00:00Z");
        Instant updated = Instant.parse("2026-10-02T00:00:00Z");

        GuideModels.GuideSummary oldSummary = new GuideModels.GuideSummary(
                1L, "CODMW4", "使命召唤", "mw4-guide", "攻略", "副标题", "摘要", "/cover.png",
                "BEGINNER", 30, "zh-CN", "星火攻略组", "官方", "https://example.com", "PUBLISHED",
                true, 10, List.of("武器", "活动"), "SEO", "SEO描述", "https://wydlgf.cn/guides/mw4-guide",
                12L, published, updated, 1, 1);
        GuideQueryModels.GuideSummary newSummary = new GuideQueryModels.GuideSummary(
                1L, "CODMW4", "使命召唤", "mw4-guide", "攻略", "副标题", "摘要", "/cover.png",
                "BEGINNER", 30, "zh-CN", "星火攻略组", "官方", "https://example.com", "PUBLISHED",
                true, 10, List.of("武器", "活动"), "SEO", "SEO描述", "https://wydlgf.cn/guides/mw4-guide",
                12L, published, updated, 1, 1);

        GuideModels.GuideDetail oldDetail = new GuideModels.GuideDetail(
                oldSummary,
                List.of(new GuideModels.GuideSection(2L, "route", "路线", "正文", null, "路线图", 10)),
                List.of(new GuideModels.GuideProduct(
                        3L, "service", "服务", "EVENT", "描述", "FIXED", new BigDecimal("35.00"),
                        null, null, "60分钟", "/service.png", "推荐", "商品描述", true, 10)),
                List.of(new GuideModels.GuideFaq(4L, "问题？", "回答。", 10)),
                List.of(new GuideModels.GuideRelated(5L, "CODMW4", "使命召唤", "related", "相关攻略", "/related.png")));
        GuideQueryModels.GuideDetail newDetail = new GuideQueryModels.GuideDetail(
                newSummary,
                List.of(new GuideQueryModels.GuideSection(2L, "route", "路线", "正文", null, "路线图", 10)),
                List.of(new GuideQueryModels.GuideProduct(
                        3L, "service", "服务", "EVENT", "描述", "FIXED", new BigDecimal("35.00"),
                        null, null, "60分钟", "/service.png", "推荐", "商品描述", true, 10)),
                List.of(new GuideQueryModels.GuideFaq(4L, "问题？", "回答。", 10)),
                List.of(new GuideQueryModels.GuideRelated(5L, "CODMW4", "使命召唤", "related", "相关攻略", "/related.png")));

        assertEquals(mapper.valueToTree(oldSummary), mapper.valueToTree(newSummary));
        assertEquals(mapper.valueToTree(oldDetail), mapper.valueToTree(newDetail));
    }
}
