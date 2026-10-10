package com.xinghuo.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.guide.application.GuideWriteModels;
import com.xinghuo.guide.web.GuideWriteWebMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuideWriteCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void webMapperPreservesEveryGuideUpsertField() {
        GuideModels.GuideUpsert input = new GuideModels.GuideUpsert(
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
                List.of(new GuideModels.SectionInput("route", "路线", "正文", "/section.png", "路线图", 10)),
                List.of(new GuideModels.ProductInput(9L, "推荐服务", "描述", "/product.png", true, 20)),
                List.of(new GuideModels.FaqInput("问题？", "回答。", 30)),
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
        assertEquals(input.sections().get(0).bodyMarkdown(), command.sections().get(0).bodyMarkdown());
        assertEquals(input.products().get(0).serviceId(), command.products().get(0).serviceId());
        assertEquals(input.products().get(0).featured(), command.products().get(0).featured());
        assertEquals(input.faqs().get(0).question(), command.faqs().get(0).question());
        assertEquals(input.faqs().get(0).answer(), command.faqs().get(0).answer());
    }

    @Test
    void batchAndImportResponsesKeepLegacyJsonShape() {
        GuideModels.GuideBatchArchiveResult oldBatch = new GuideModels.GuideBatchArchiveResult(3, 1);
        GuideWriteModels.GuideBatchArchiveResult newBatch = new GuideWriteModels.GuideBatchArchiveResult(3, 1);
        assertEquals(mapper.valueToTree(oldBatch), mapper.valueToTree(newBatch));

        GuideModels.GuideImportResult oldImport = new GuideModels.GuideImportResult(10, 4, 3, 3, List.of(1L, 2L));
        GuideWriteModels.GuideImportResult newImport = new GuideWriteModels.GuideImportResult(10, 4, 3, 3, List.of(1L, 2L));
        assertEquals(mapper.valueToTree(oldImport), mapper.valueToTree(newImport));
    }
}
