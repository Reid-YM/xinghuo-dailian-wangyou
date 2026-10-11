package com.xinghuo.taxonomy.web;

import com.xinghuo.taxonomy.application.TaxonomyCatalogView;
import com.xinghuo.taxonomy.application.TaxonomyCommands;
import java.util.List;

/** Maps taxonomy HTTP DTOs to and from application-owned contracts. */
public final class TaxonomyWebMapper {
    private TaxonomyWebMapper() {
    }

    public static TaxonomyCommands.CreateGame toCommand(CatalogNameRequest request) {
        return new TaxonomyCommands.CreateGame(
                request.name(), request.iconKey(), request.description(), request.sortOrder(),
                request.active(), request.reason());
    }

    public static TaxonomyCommands.UpdateGame toCommand(CatalogUpdateRequest request) {
        return new TaxonomyCommands.UpdateGame(
                request.name(), request.iconKey(), request.description(), request.sortOrder(),
                request.active(), request.reason());
    }

    public static TaxonomyCommands.UpdateGameProfile toCommand(GameProfileUpdateRequest request) {
        return new TaxonomyCommands.UpdateGameProfile(
                request.name(), request.nameEn(), request.aliases(), request.iconKey(),
                request.description(), request.publisher(), request.genres(), request.logoImage(),
                request.coverImage(), request.popularityScore(), request.featured(),
                request.businessStatus(), request.seoSlug(), request.seoTitle(),
                request.seoDescription(), request.sortOrder(), request.active(),
                request.platformCodes(), request.reason());
    }

    public static TaxonomyCommands.BulkUpdateGames toCommand(GameBulkUpdateRequest request) {
        return new TaxonomyCommands.BulkUpdateGames(
                request.codes(), request.active(), request.featured(), request.businessStatus(),
                request.popularityScore(), request.reason());
    }

    public static TaxonomyCommands.CreateCategory toCommand(CategoryCreateRequest request) {
        return new TaxonomyCommands.CreateCategory(
                request.gameCode(), request.name(), request.iconKey(), request.description(),
                request.sortOrder(), request.active(), request.reason());
    }

    public static TaxonomyCommands.UpdateCategory toCommand(CategoryUpdateRequest request) {
        return new TaxonomyCommands.UpdateCategory(
                request.name(), request.iconKey(), request.description(), request.sortOrder(),
                request.active(), request.reason());
    }

    public static CatalogResponse toResponse(TaxonomyCatalogView view) {
        return new CatalogResponse(
                view.games().stream().map(TaxonomyWebMapper::toResponse).toList(),
                view.categories().stream().map(TaxonomyWebMapper::toResponse).toList(),
                view.platforms().stream().map(TaxonomyWebMapper::toResponse).toList(),
                view.gamePlatforms().stream().map(TaxonomyWebMapper::toResponse).toList());
    }

    public static GameOptionResponse toResponse(TaxonomyCatalogView.GameView view) {
        return new GameOptionResponse(
                view.code(), view.name(), view.nameEn(), view.aliases(), view.iconKey(),
                view.description(), view.publisher(), view.genres(), view.logoImage(),
                view.coverImage(), view.popularityScore(), view.featured(), view.businessStatus(),
                view.seoSlug(), view.seoTitle(), view.seoDescription(), view.active(),
                view.sortOrder(), view.categoryCount(), view.serviceCount(), view.platformCodes());
    }

    public static List<GameOptionResponse> toGameResponses(
            List<TaxonomyCatalogView.GameView> views) {
        return views.stream().map(TaxonomyWebMapper::toResponse).toList();
    }

    public static CategoryOptionResponse toResponse(TaxonomyCatalogView.CategoryView view) {
        return new CategoryOptionResponse(
                view.id(), view.gameCode(), view.code(), view.name(), view.iconKey(),
                view.description(), view.active(), view.sortOrder(), view.serviceCount());
    }

    public static PlatformOptionResponse toResponse(TaxonomyCatalogView.PlatformView view) {
        return new PlatformOptionResponse(
                view.code(), view.name(), view.iconKey(), view.platformGroup(), view.description(),
                view.active(), view.sortOrder());
    }

    public static GamePlatformResponse toResponse(TaxonomyCatalogView.GamePlatformView view) {
        return new GamePlatformResponse(
                view.gameCode(), view.platformCode(), view.active(), view.sortOrder());
    }
}
