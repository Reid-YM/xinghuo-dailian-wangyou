package com.xinghuo.guide.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Persistence-independent guide projections used by public, admin, and page reads. */
public final class GuideQueryModels {
    private GuideQueryModels() {
    }

    public record GuideSection(
            Long id,
            String sectionKey,
            String heading,
            String bodyMarkdown,
            String imageUrl,
            String imageAlt,
            int sortOrder) {
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
            int sortOrder) {
    }

    public record GuideFaq(Long id, String question, String answer, int sortOrder) {
    }

    public record GuideRelated(
            Long id,
            String gameCode,
            String gameName,
            String slug,
            String title,
            String coverImage) {
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
            int productCount) {
    }

    public record GuideDetail(
            GuideSummary guide,
            List<GuideSection> sections,
            List<GuideProduct> products,
            List<GuideFaq> faqs,
            List<GuideRelated> relatedGuides) {
    }
}
