package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class XingweiTaxonomyControlServiceTest {

    @Test
    void gameImpactShowsCategoryServiceAndPublicCandidateCounts() {
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        XingweiTaxonomyControlService service = new XingweiTaxonomyControlService(taxonomy, services);
        ApiDtos.CatalogResponse catalog = mock(ApiDtos.CatalogResponse.class);
        ApiDtos.GameOptionResponse game = mock(ApiDtos.GameOptionResponse.class);
        ApiDtos.CategoryOptionResponse activeCategory = mock(ApiDtos.CategoryOptionResponse.class);
        ApiDtos.CategoryOptionResponse inactiveCategory = mock(ApiDtos.CategoryOptionResponse.class);
        when(taxonomy.normalizeGame("mw4")).thenReturn("MW4");
        when(taxonomy.catalog(true)).thenReturn(catalog);
        when(catalog.games()).thenReturn(List.of(game));
        when(catalog.categories()).thenReturn(List.of(activeCategory, inactiveCategory));
        when(game.code()).thenReturn("MW4");
        when(game.name()).thenReturn("Modern Warfare 4");
        when(game.active()).thenReturn(true);
        when(game.categoryCount()).thenReturn(2L);
        when(game.serviceCount()).thenReturn(3L);
        when(activeCategory.gameCode()).thenReturn("MW4");
        when(activeCategory.active()).thenReturn(true);
        when(inactiveCategory.gameCode()).thenReturn("MW4");
        when(inactiveCategory.active()).thenReturn(false);
        when(services.adminList()).thenReturn(List.of(
                serviceRow("MW4", "A", true, "AVAILABLE", null),
                serviceRow("MW4", "A", true, "OFFLINE", null),
                serviceRow("MW4", "B", true, "AVAILABLE", Instant.now().minusSeconds(60))));

        XingweiTaxonomyControlService.TaxonomyImpact impact = service.gameImpact("mw4");

        assertEquals("GAME", impact.type());
        assertEquals(2L, impact.categoryCount());
        assertEquals(1L, impact.activeCategoryCount());
        assertEquals(3L, impact.serviceCount());
        assertEquals(1L, impact.publicCandidateServiceCount());
        assertTrue(impact.active());
    }

    @Test
    void gameTogglePreservesMetadataAndInvalidatesVisibilityCache() {
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        XingweiTaxonomyControlService service = new XingweiTaxonomyControlService(taxonomy, services);
        ApiDtos.CatalogResponse catalog = mock(ApiDtos.CatalogResponse.class);
        ApiDtos.GameOptionResponse game = mock(ApiDtos.GameOptionResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(taxonomy.normalizeGame("MW4")).thenReturn("MW4");
        when(taxonomy.catalog(true)).thenReturn(catalog);
        when(catalog.games()).thenReturn(List.of(game));
        when(catalog.categories()).thenReturn(List.of());
        when(game.code()).thenReturn("MW4");
        when(game.name()).thenReturn("Modern Warfare 4");
        when(game.iconKey()).thenReturn("gamepad");
        when(game.description()).thenReturn("desc");
        when(game.sortOrder()).thenReturn(20);
        when(game.active()).thenReturn(true);
        when(game.categoryCount()).thenReturn(0L);
        when(game.serviceCount()).thenReturn(0L);
        when(services.adminList()).thenReturn(List.of());
        when(taxonomy.updateGame(eq("MW4"), any(), eq("operator"), eq(request))).thenReturn(game);

        service.setGameActive("MW4", false, "暂时停止接单", "operator", request);

        ArgumentCaptor<ApiDtos.CatalogUpdateRequest> captor = ArgumentCaptor.forClass(ApiDtos.CatalogUpdateRequest.class);
        verify(taxonomy).updateGame(eq("MW4"), captor.capture(), eq("operator"), eq(request));
        ApiDtos.CatalogUpdateRequest update = captor.getValue();
        assertEquals("Modern Warfare 4", update.name());
        assertEquals("gamepad", update.iconKey());
        assertEquals("desc", update.description());
        assertEquals(20, update.sortOrder());
        assertFalse(update.active());
        assertEquals("暂时停止接单", update.reason());
        verify(taxonomy).invalidateVisibilityCache();
    }

    @Test
    void categoryTogglePreservesMetadataAndInvalidatesVisibilityCache() {
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        XingweiTaxonomyControlService service = new XingweiTaxonomyControlService(taxonomy, services);
        ApiDtos.CatalogResponse catalog = mock(ApiDtos.CatalogResponse.class);
        ApiDtos.CategoryOptionResponse category = mock(ApiDtos.CategoryOptionResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(taxonomy.catalog(true)).thenReturn(catalog);
        when(catalog.categories()).thenReturn(List.of(category));
        when(category.id()).thenReturn(9L);
        when(category.gameCode()).thenReturn("MW4");
        when(category.code()).thenReturn("LEVELING");
        when(category.name()).thenReturn("升级");
        when(category.iconKey()).thenReturn("rank");
        when(category.description()).thenReturn("desc");
        when(category.sortOrder()).thenReturn(30);
        when(category.active()).thenReturn(false);
        when(category.serviceCount()).thenReturn(2L);
        when(services.adminList()).thenReturn(List.of());
        when(taxonomy.updateCategory(eq(9L), any(), eq("operator"), eq(request))).thenReturn(category);

        service.setCategoryActive(9L, true, "恢复该分类", "operator", request);

        ArgumentCaptor<ApiDtos.CategoryUpdateRequest> captor = ArgumentCaptor.forClass(ApiDtos.CategoryUpdateRequest.class);
        verify(taxonomy).updateCategory(eq(9L), captor.capture(), eq("operator"), eq(request));
        ApiDtos.CategoryUpdateRequest update = captor.getValue();
        assertEquals("升级", update.name());
        assertEquals("rank", update.iconKey());
        assertEquals("desc", update.description());
        assertEquals(30, update.sortOrder());
        assertTrue(update.active());
        assertEquals("恢复该分类", update.reason());
        verify(taxonomy).invalidateVisibilityCache();
    }

    private ApiDtos.AdminServiceResponse serviceRow(
            String gameCode,
            String category,
            boolean active,
            String status,
            Instant expiresAt) {
        return new ApiDtos.AdminServiceResponse(
                1L,
                "slug-" + category,
                gameCode,
                "service",
                category,
                "desc",
                "FIXED",
                BigDecimal.TEN,
                null,
                null,
                "1 day",
                status,
                null,
                BigDecimal.ZERO,
                0,
                expiresAt,
                active,
                0,
                0L);
    }
}
