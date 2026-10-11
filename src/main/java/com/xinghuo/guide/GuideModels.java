package com.xinghuo.guide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class GuideModels {
    private GuideModels() {
    }

    public record GuideSection(
            Long id,
            String sectionKey,
            String heading,
            String bodyMarkdown,
            String imageUrl,
            String imageAlt,
            int sortOrder
    ) {
    }

    public record GuideProduct(
            Long serviceId,
            String slug,
            String name,
            String category,
            String shortDesc,
            String priceMode,
            BigDecimal priceFixed,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String durationText,
            String imageUrl,
            String label,
            String description,
            boolean featured,
            int sortOrder
    ) {
    }

    public record GuideFaq(
            Long id,
            String question,
            String answer,
            int sortOrder
    ) {
    }

    public record GuideRelated(
            Long id,
            String gameCode,
            String gameName,
            String slug,
            String title,
            String coverImage
    ) {
    }

    public record GuideSummary(
            Long id,
            String gameCode,
            String gameName,
            String slug,
            String title,
            String subtitle,
            String summary,
            String coverImage,
            String difficulty,
            int estimatedMinutes,
            String language,
            String authorName,
            String sourceName,
            String sourceUrl,
            String status,
            boolean featured,
            int sortOrder,
            List<String> tags,
            String seoTitle,
            String seoDescription,
            String canonicalUrl,
            long viewCount,
            Instant publishedAt,
            Instant updatedAt,
            int sectionCount,
            int productCount
    ) {
    }

    public record GuideDetail(
            GuideSummary guide,
            List<GuideSection> sections,
            List<GuideProduct> products,
            List<GuideFaq> faqs,
            List<GuideRelated> relatedGuides
    ) {
    }

    public record SectionInput(
            String sectionKey,
            String heading,
            String bodyMarkdown,
            String imageUrl,
            String imageAlt,
            Integer sortOrder
    ) {
    }

    public record ProductInput(
            Long serviceId,
            String label,
            String description,
            String imageUrl,
            Boolean featured,
            Integer sortOrder
    ) {
    }

    public record FaqInput(
            String question,
            String answer,
            Integer sortOrder
    ) {
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
            String reason
    ) {
    }

    public record GuideStatusInput(String status, String reason) {
    }

    public record GuideBatchArchiveRequest(List<Long> ids, String reason) {
    }

    public record GuideBatchArchiveResult(int archived, int skipped) {
    }

    public record GuideImportRequest(Boolean publish, Boolean refresh, String reason) {
    }

    public record GuideImportResult(
            int totalCandidates,
            int created,
            int refreshed,
            int skipped,
            List<Long> guideIds
    ) {
    }
}
