package com.xinghuo.guide.application;

import java.util.List;

public final class GuideWriteModels {
    private GuideWriteModels() {
    }

    public record SectionInput(
            String sectionKey,
            String heading,
            String bodyMarkdown,
            String imageUrl,
            String imageAlt,
            Integer sortOrder) {
    }

    public record ProductInput(
            Long serviceId,
            String label,
            String description,
            String imageUrl,
            Boolean featured,
            Integer sortOrder) {
    }

    public record FaqInput(String question, String answer, Integer sortOrder) {
    }

    public record GuideUpsertCommand(
            String gameCode,
            String slug,
            String title,
            String subtitle,
            String summary,
            String coverImage,
            String difficulty,
            Integer estimatedMinutes,
            String language,
            String authorName,
            String sourceName,
            String sourceUrl,
            String status,
            Boolean featured,
            Integer sortOrder,
            List<String> tags,
            String seoTitle,
            String seoDescription,
            String canonicalUrl,
            List<SectionInput> sections,
            List<ProductInput> products,
            List<FaqInput> faqs,
            List<Long> relatedGuideIds,
            String reason) {
    }

    public record GuideBatchArchiveResult(int archived, int skipped) {
    }

    public record GuideImportCommand(Boolean publish, Boolean refresh, String reason) {
    }

    public record GuideImportResult(
            int totalCandidates,
            int created,
            int refreshed,
            int skipped,
            List<Long> guideIds) {
    }
}
