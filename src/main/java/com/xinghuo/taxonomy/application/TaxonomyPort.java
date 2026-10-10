package com.xinghuo.taxonomy.application;

import java.util.List;

/** Taxonomy-owned persistence and audit boundary. */
public interface TaxonomyPort {
    TaxonomyCatalogView catalog(boolean admin);

    TaxonomyCatalogView.GameView publicGameBySeoSlug(String seoSlug);

    List<TaxonomyCatalogView.GameView> publicGames();

    TaxonomyCatalogView.GameView addGame(TaxonomyCommands.CreateGame command, String operator);

    TaxonomyCatalogView.GameView updateGame(String code, TaxonomyCommands.UpdateGame command, String operator);

    TaxonomyCatalogView.GameView updateGameProfile(
            String code, TaxonomyCommands.UpdateGameProfile command, String operator);

    List<TaxonomyCatalogView.GameView> bulkUpdateGames(
            TaxonomyCommands.BulkUpdateGames command, String operator);

    TaxonomyCatalogView.CategoryView addCategory(
            TaxonomyCommands.CreateCategory command, String operator);

    TaxonomyCatalogView.CategoryView updateCategory(
            Long id, TaxonomyCommands.UpdateCategory command, String operator);

    void deleteGame(String code, String operator);

    void deleteCategory(Long id, String operator);

    void validate(String game, String category);

    boolean publicAllowed(String game, String category);

    void invalidateVisibilityCache();
}
