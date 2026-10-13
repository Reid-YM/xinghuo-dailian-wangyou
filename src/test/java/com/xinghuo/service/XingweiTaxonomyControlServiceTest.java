package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class XingweiTaxonomyControlServiceTest {

    @Test
    void gameImpactShowsVisibilityCountsWarningsAndStableToken() {
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
        when(activeCategory.code()).thenReturn("A");
        when(activeCategory.active()).thenReturn(true);
        when(inactiveCategory.gameCode()).thenReturn("MW4");
        when(inactiveCategory.code()).thenReturn("B");
        when(inactiveCategory.active()).thenReturn(false);
        when(services.adminList()).thenReturn(List.of(
                serviceRow(1L, "MW4", "A", true, "AVAILABLE", null),
                serviceRow(2L, "MW4", "A", true, "OFFLINE", null),
                serviceRow(3L, "MW4", "B", true, "AVAILABLE", Instant.now().minusSeconds(60))));

        XingweiTaxonomyControlService.TaxonomyImpact impact = service.gameImpact("mw4");

        assertEquals("GAME", impact.type());
        assertEquals(2L, impact.categoryCount());
        assertEquals(1L, impact.activeCategoryCount());
        assertEquals(3L, impact.serviceCount());
        assertEquals(1L, impact.publicCandidateServiceCount());
        assertEquals(1L, impact.currentPublicServiceCount());
        assertEquals(0L, impact.targetPublicServiceCount());
        assertTrue(impact.visibilitySummary().contains("1 个公开服务"));
        assertTrue(impact.warnings().stream().anyMatch(value -> value.contains("隐藏 1 个")));
        assertEquals(32, impact.impactToken().length());
        assertTrue(impact.active());
    }

    @Test
    void gameTogglePreservesMetadataChecksTokenAndInvalidatesVisibilityCache() {
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
        String token = service.gameImpact("MW4").impactToken();

        service.setGameActive("MW4", false, "暂时停止接单", token, "operator", request);

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
    void staleImpactTokenRejectsToggleBeforeWrite() {
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
        when(game.active()).thenReturn(true);
        when(game.categoryCount()).thenReturn(0L);
        when(game.serviceCount()).thenReturn(0L);
        when(services.adminList()).thenReturn(List.of());

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.setGameActive("MW4", false, "reason", "stale-token", "operator", request));

        assertTrue(error.getMessage().contains("重新查看影响"));
        verify(taxonomy, never()).updateGame(any(), any(), any(), any());
    }

    @Test
    void categoryEnableExplainsParentGameBlockAndZeroTargetVisibility() {
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        XingweiTaxonomyControlService service = new XingweiTaxonomyControlService(taxonomy, services);
        ApiDtos.CatalogResponse catalog = mock(ApiDtos.CatalogResponse.class);
        ApiDtos.CategoryOptionResponse category = mock(ApiDtos.CategoryOptionResponse.class);
        ApiDtos.GameOptionResponse parent = mock(ApiDtos.GameOptionResponse.class);
        when(taxonomy.catalog(true)).thenReturn(catalog);
        when(catalog.categories()).thenReturn(List.of(category));
        when(catalog.games()).thenReturn(List.of(parent));
        when(category.id()).thenReturn(9L);
        when(category.gameCode()).thenReturn("MW4");
        when(category.code()).thenReturn("LEVELING");
        when(category.name()).thenReturn("升级");
        when(category.active()).thenReturn(false);
        when(category.serviceCount()).thenReturn(1L);
        when(parent.code()).thenReturn("MW4");
        when(parent.name()).thenReturn("Modern Warfare 4");
        when(parent.active()).thenReturn(false);
        when(services.adminList()).thenReturn(List.of(serviceRow(8L, "MW4", "LEVELING", true, "AVAILABLE", null)));

        XingweiTaxonomyControlService.TaxonomyImpact impact = service.categoryImpact(9L);

        assertFalse(impact.parentActive());
        assertEquals(0L, impact.currentPublicServiceCount());
        assertEquals(0L, impact.targetPublicServiceCount());
        assertTrue(impact.visibilitySummary().contains("所属游戏"));
        assertTrue(impact.warnings().stream().anyMatch(value -> value.contains("需先启用游戏")));
    }

    @Test
    void categoryTogglePreservesMetadataAndInvalidatesVisibilityCache() {
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        XingweiTaxonomyControlService service = new XingweiTaxonomyControlService(taxonomy, services);
        ApiDtos.CatalogResponse catalog = mock(ApiDtos.CatalogResponse.class);
        ApiDtos.CategoryOptionResponse category = mock(ApiDtos.CategoryOptionResponse.class);
        ApiDtos.GameOptionResponse parent = mock(ApiDtos.GameOptionResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(taxonomy.catalog(true)).thenReturn(catalog);
        when(catalog.categories()).thenReturn(List.of(category));
        when(catalog.games()).thenReturn(List.of(parent));
        when(category.id()).thenReturn(9L);
        when(category.gameCode()).thenReturn("MW4");
        when(category.code()).thenReturn("LEVELING");
        when(category.name()).thenReturn("升级");
        when(category.iconKey()).thenReturn("rank");
        when(category.description()).thenReturn("desc");
        when(category.sortOrder()).thenReturn(30);
        when(category.active()).thenReturn(false);
        when(category.serviceCount()).thenReturn(2L);
        when(parent.code()).thenReturn("MW4");
        when(parent.name()).thenReturn("Modern Warfare 4");
        when(parent.active()).thenReturn(true);
        when(services.adminList()).thenReturn(List.of());
        when(taxonomy.updateCategory(eq(9L), any(), eq("operator"), eq(request))).thenReturn(category);
        String token = service.categoryImpact(9L).impactToken();

        service.setCategoryActive(9L, true, "恢复该分类", token, "operator", request);

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
            Long id,
            String gameCode,
            String category,
            boolean active,
            String status,
            Instant expiresAt) {
        return new ApiDtos.AdminServiceResponse(
                id,
                "slug-" + id,
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
