package com.xinghuo.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.guide.application.GuideWriteModels;
import com.xinghuo.guide.web.GuideAdminRequests;
import com.xinghuo.guide.web.GuideWriteWebMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuideWriteCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void webMapperPreservesEveryGuideUpsertField() {
        GuideAdminRequests.GuideUpsert input = new GuideAdminRequests.GuideUpsert(
                "CODMW4",
                "mw4-route",
                "标题",
                "副标题",
                "摘要",
                "/cover.png",
                "ADVANCED",
                90,
                "zh-CN",
                "作者",
                "来源",
                "https://example.com/source",
                "PUBLISHED",
                true,
                30,
                List.of("活动", "武器"),
                "SEO标题",
                "SEO描述",
                "https://wydlgf.cn/guides/mw4-route",
                List.of(new GuideAdminRequests.SectionInput("route", "路线", "正文", "/section.png", "路线图", 10)),
                List.of(new GuideAdminRequests.ProductInput(9L, "推荐服务", "描述", "/product.png", true, 20)),
                List.of(new GuideAdminRequests.FaqInput("问题？", "回答。", 30)),
                List.of(11L, 12L),
                "测试更新");

        GuideWriteModels.GuideUpsertCommand command = GuideWriteWebMapper.toCommand(input);

        assertEquals(input.gameCode(), command.gameCode());
        assertEquals(input.slug(), command.slug());
        assertEquals(input.title(), command.title());
        assertEquals(input.subtitle(), command.subtitle());
        assertEquals(input.summary(), command.summary());
        assertEquals(input.coverImage(), command.coverImage());
        assertEquals(input.difficulty(), command.difficulty());
        assertEquals(input.estimatedMinutes(), command.estimatedMinutes());
        assertEquals(input.language(), command.language());
        assertEquals(input.authorName(), command.authorName());
        assertEquals(input.sourceName(), command.sourceName());
        assertEquals(input.sourceUrl(), command.sourceUrl());
        assertEquals(input.status(), command.status());
        assertEquals(input.featured(), command.featured());
        assertEquals(input.sortOrder(), command.sortOrder());
        assertEquals(input.tags(), command.tags());
        assertEquals(input.seoTitle(), command.seoTitle());
        assertEquals(input.seoDescription(), command.seoDescription());
        assertEquals(input.canonicalUrl(), command.canonicalUrl());
        assertEquals(input.relatedGuideIds(), command.relatedGuideIds());
        assertEquals(input.reason(), command.reason());
        assertEquals(input.sections().get(0).sectionKey(), command.sections().get(0).sectionKey());
        assertEquals(input.products().get(0).serviceId(), command.products().get(0).serviceId());
        assertEquals(input.faqs().get(0).question(), command.faqs().get(0).question());
    }

    @Test
    void adminRequestAndWriteResponseJsonFieldsStayStable() {
        GuideAdminRequests.GuideImportRequest request =
                new GuideAdminRequests.GuideImportRequest(true, false, "导入");
        assertEquals(Set.of("publish", "refresh", "reason"), fields(mapper.valueToTree(request)));

        GuideWriteModels.GuideBatchArchiveResult batch = new GuideWriteModels.GuideBatchArchiveResult(3, 1);
        assertEquals(Set.of("archived", "skipped"), fields(mapper.valueToTree(batch)));

        GuideWriteModels.GuideImportResult imported =
                new GuideWriteModels.GuideImportResult(10, 4, 3, 3, List.of(1L, 2L));
        JsonNode importJson = mapper.valueToTree(imported);
        assertEquals(Set.of("totalCandidates", "created", "refreshed", "skipped", "guideIds"), fields(importJson));
        assertEquals(10, importJson.get("totalCandidates").asInt());
    }

    private Set<String> fields(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
