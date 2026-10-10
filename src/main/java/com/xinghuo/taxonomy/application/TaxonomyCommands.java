package com.xinghuo.taxonomy.application;

import java.util.List;

/** Application-owned administrative commands for taxonomy maintenance. */
public final class TaxonomyCommands {
    private TaxonomyCommands() {
    }

    public record CreateGame(
            String name,
            String iconKey,
            String description,
            Integer sortOrder,
            Boolean active,
            String reason) {
    }

    public record UpdateGame(
            String name,
            String iconKey,
            String description,
            Integer sortOrder,
            Boolean active,
            String reason) {
    }

    public record UpdateGameProfile(
            String name,
            String nameEn,
            String aliases,
            String iconKey,
            String description,
            String publisher,
            String genres,
            String logoImage,
            String coverImage,
            Integer popularityScore,
            Boolean featured,
            String businessStatus,
            String seoSlug,
            String seoTitle,
            String seoDescription,
            Integer sortOrder,
            Boolean active,
            List<String> platformCodes,
            String reason) {
    }

    public record BulkUpdateGames(
            List<String> codes,
            Boolean active,
            Boolean featured,
            String businessStatus,
            Integer popularityScore,
            String reason) {
    }

    public record CreateCategory(
            String gameCode,
            String name,
            String iconKey,
            String description,
            Integer sortOrder,
            Boolean active,
            String reason) {
    }

    public record UpdateCategory(
            String name,
            String iconKey,
            String description,
            Integer sortOrder,
            Boolean active,
            String reason) {
    }
}
