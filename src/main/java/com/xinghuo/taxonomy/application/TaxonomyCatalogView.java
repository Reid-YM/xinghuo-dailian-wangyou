package com.xinghuo.taxonomy.application;

import java.util.List;

/** Application-owned game/category/platform catalog projection. */
public record TaxonomyCatalogView(
        List<GameView> games,
        List<CategoryView> categories,
        List<PlatformView> platforms,
        List<GamePlatformView> gamePlatforms) {

    public record GameView(
            String code,
            String name,
            String nameEn,
            String aliases,
            String iconKey,
            String description,
            String publisher,
            String genres,
            String logoImage,
            String coverImage,
            int popularityScore,
            boolean featured,
            String businessStatus,
            String seoSlug,
            String seoTitle,
            String seoDescription,
            boolean active,
            int sortOrder,
            long categoryCount,
            long serviceCount,
            List<String> platformCodes) {
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getNameEn() { return nameEn; }
        public String getAliases() { return aliases; }
        public String getIconKey() { return iconKey; }
        public String getDescription() { return description; }
        public String getPublisher() { return publisher; }
        public String getGenres() { return genres; }
        public String getLogoImage() { return logoImage; }
        public String getCoverImage() { return coverImage; }
        public int getPopularityScore() { return popularityScore; }
        public boolean isFeatured() { return featured; }
        public String getBusinessStatus() { return businessStatus; }
        public String getSeoSlug() { return seoSlug; }
        public String getSeoTitle() { return seoTitle; }
        public String getSeoDescription() { return seoDescription; }
        public boolean isActive() { return active; }
        public int getSortOrder() { return sortOrder; }
        public long getCategoryCount() { return categoryCount; }
        public long getServiceCount() { return serviceCount; }
        public List<String> getPlatformCodes() { return platformCodes; }
    }

    public record CategoryView(
            Long id,
            String gameCode,
            String code,
            String name,
            String iconKey,
            String description,
            boolean active,
            int sortOrder,
            long serviceCount) {
    }

    public record PlatformView(
            String code,
            String name,
            String iconKey,
            String platformGroup,
            String description,
            boolean active,
            int sortOrder) {
    }

    public record GamePlatformView(
            String gameCode,
            String platformCode,
            boolean active,
            int sortOrder) {
    }
}
