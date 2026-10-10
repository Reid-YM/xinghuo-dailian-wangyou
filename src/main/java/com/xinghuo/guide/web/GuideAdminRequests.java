package com.xinghuo.guide.web;

import java.util.List;

/** HTTP request contracts for guide administration. */
public final class GuideAdminRequests {
    private GuideAdminRequests() {
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

    public record GuideUpsert(
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

    public record GuideStatusInput(String status, String reason) {
    }

    public record GuideBatchArchiveRequest(List<Long> ids, String reason) {
    }

    public record GuideImportRequest(Boolean publish, Boolean refresh, String reason) {
    }
}
