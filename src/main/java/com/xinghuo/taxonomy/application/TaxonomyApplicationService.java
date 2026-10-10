package com.xinghuo.taxonomy.application;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Application boundary for games, categories, platforms, and taxonomy validation. */
@Service
public class TaxonomyApplicationService {
    private final TaxonomyPort port;

    public TaxonomyApplicationService(TaxonomyPort port) {
        this.port = port;
    }

    public TaxonomyCatalogView catalog(boolean admin) {
        return port.catalog(admin);
    }

    public TaxonomyCatalogView.GameView publicGameBySeoSlug(String seoSlug) {
        return port.publicGameBySeoSlug(seoSlug);
    }

    public List<TaxonomyCatalogView.GameView> publicGames() {
        return port.publicGames();
    }

    public TaxonomyCatalogView.GameView addGame(TaxonomyCommands.CreateGame command, String operator) {
        return port.addGame(command, operator);
    }

    public TaxonomyCatalogView.GameView updateGame(
            String code, TaxonomyCommands.UpdateGame command, String operator) {
        return port.updateGame(code, command, operator);
    }

    public TaxonomyCatalogView.GameView updateGameProfile(
            String code, TaxonomyCommands.UpdateGameProfile command, String operator) {
        return port.updateGameProfile(code, command, operator);
    }

    public List<TaxonomyCatalogView.GameView> bulkUpdateGames(
            TaxonomyCommands.BulkUpdateGames command, String operator) {
        return port.bulkUpdateGames(command, operator);
    }

    public TaxonomyCatalogView.CategoryView addCategory(
            TaxonomyCommands.CreateCategory command, String operator) {
        return port.addCategory(command, operator);
    }

    public TaxonomyCatalogView.CategoryView updateCategory(
            Long id, TaxonomyCommands.UpdateCategory command, String operator) {
        return port.updateCategory(id, command, operator);
    }

    public void deleteGame(String code, String operator) {
        port.deleteGame(code, operator);
    }

    public void deleteCategory(Long id, String operator) {
        port.deleteCategory(id, operator);
    }

    public String normalizeGame(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    public void validate(String game, String category) {
        port.validate(game, category);
    }

    public boolean publicAllowed(String game, String category) {
        return port.publicAllowed(game, category);
    }

    public void invalidateVisibilityCache() {
        port.invalidateVisibilityCache();
    }
}
