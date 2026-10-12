package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceCategoryCatalog;
import com.xinghuo.repository.GameCatalogRepository;
import com.xinghuo.repository.GamePlatformRepository;
import com.xinghuo.repository.PlatformCatalogRepository;
import com.xinghuo.repository.ServiceCategoryCatalogRepository;
import com.xinghuo.repository.ServiceItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxonomyVisibilityCacheCharacterizationTest {

    @Test
    void publicAllowedCachesActiveGameCategoryPairsUntilInvalidated() {
        GameCatalogRepository games = mock(GameCatalogRepository.class);
        ServiceCategoryCatalogRepository categories = mock(ServiceCategoryCatalogRepository.class);
        when(games.findAllByOrderBySortOrderAscCodeAsc()).thenReturn(List.of(game("MW4", true)));
        when(categories.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(category("MW4", "CAMO", true)));
        TaxonomyService service = service(games, categories);

        assertTrue(service.publicAllowed(" mw4 ", " camo "));
        assertTrue(service.publicAllowed("MW4", "CAMO"));
        verify(games, times(1)).findAllByOrderBySortOrderAscCodeAsc();
        verify(categories, times(1)).findAllByOrderBySortOrderAscIdAsc();

        service.invalidateVisibilityCache();

        assertTrue(service.publicAllowed("MW4", "CAMO"));
        verify(games, times(2)).findAllByOrderBySortOrderAscCodeAsc();
        verify(categories, times(2)).findAllByOrderBySortOrderAscIdAsc();
    }

    @Test
    void publicAllowedExcludesCategoriesBelongingToInactiveGames() {
        GameCatalogRepository games = mock(GameCatalogRepository.class);
        ServiceCategoryCatalogRepository categories = mock(ServiceCategoryCatalogRepository.class);
        when(games.findAllByOrderBySortOrderAscCodeAsc()).thenReturn(List.of(game("MW4", false)));
        when(categories.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(category("MW4", "CAMO", true)));
        TaxonomyService service = service(games, categories);

        assertFalse(service.publicAllowed("MW4", "CAMO"));
    }

    private TaxonomyService service(
            GameCatalogRepository games,
            ServiceCategoryCatalogRepository categories) {
        return new TaxonomyService(
                games,
                categories,
                mock(ServiceItemRepository.class),
                mock(PlatformCatalogRepository.class),
                mock(GamePlatformRepository.class),
                mock(AuditService.class));
    }

    private GameCatalog game(String code, boolean active) {
        return GameCatalog.builder()
                .code(code)
                .name(code)
                .active(active)
                .sortOrder(10)
                .build();
    }

    private ServiceCategoryCatalog category(String gameCode, String code, boolean active) {
        return ServiceCategoryCatalog.builder()
                .gameCode(gameCode)
                .code(code)
                .name(code)
                .active(active)
                .sortOrder(10)
                .build();
    }
}
