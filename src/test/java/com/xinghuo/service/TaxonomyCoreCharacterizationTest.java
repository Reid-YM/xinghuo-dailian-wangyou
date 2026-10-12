package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceCategoryCatalog;
import com.xinghuo.repository.GameCatalogRepository;
import com.xinghuo.repository.GamePlatformRepository;
import com.xinghuo.repository.PlatformCatalogRepository;
import com.xinghuo.repository.ServiceCategoryCatalogRepository;
import com.xinghuo.repository.ServiceItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxonomyCoreCharacterizationTest {
    @Mock
    private GameCatalogRepository games;
    @Mock
    private ServiceCategoryCatalogRepository categories;
    @Mock
    private ServiceItemRepository services;
    @Mock
    private PlatformCatalogRepository platforms;
    @Mock
    private GamePlatformRepository gamePlatforms;
    @Mock
    private AuditService audit;

    private TaxonomyService service;

    @BeforeEach
    void setUp() {
        service = new TaxonomyService(games, categories, services, platforms, gamePlatforms, audit);
    }

    @Test
    void publicGameBySeoSlugReturnsActiveGame() {
        GameCatalog game = mock(GameCatalog.class);
        when(game.isActive()).thenReturn(true);
        when(games.findBySeoSlug("modern-warfare-4")).thenReturn(Optional.of(game));

        assertSame(game, service.publicGameBySeoSlug("modern-warfare-4"));
    }

    @Test
    void publicGameBySeoSlugHidesInactiveGame() {
        GameCatalog game = mock(GameCatalog.class);
        when(game.isActive()).thenReturn(false);
        when(games.findBySeoSlug("hidden-game")).thenReturn(Optional.of(game));

        assertThrows(NotFoundException.class, () -> service.publicGameBySeoSlug("hidden-game"));
    }

    @Test
    void publicGamesPreservesRepositoryOrderWhileFilteringInactiveRows() {
        GameCatalog first = mock(GameCatalog.class);
        GameCatalog hidden = mock(GameCatalog.class);
        GameCatalog third = mock(GameCatalog.class);
        when(first.isActive()).thenReturn(true);
        when(hidden.isActive()).thenReturn(false);
        when(third.isActive()).thenReturn(true);
        when(games.findAllByOrderBySortOrderAscCodeAsc()).thenReturn(List.of(first, hidden, third));

        assertEquals(List.of(first, third), service.publicGames());
    }

    @Test
    void deleteGameRejectsDependenciesBeforeDeletingOrAuditing() {
        GameCatalog game = mock(GameCatalog.class);
        when(games.findById("CODMW4")).thenReturn(Optional.of(game));
        when(categories.countByGameCode("CODMW4")).thenReturn(1L);
        when(services.countByGameCode("CODMW4")).thenReturn(0L);

        assertThrows(ConflictException.class, () -> service.deleteGame(" codmw4 ", "admin", null));

        verify(gamePlatforms, never()).deleteByGameCode(any());
        verify(games, never()).delete(any());
        verifyNoInteractions(audit);
    }

    @Test
    void deleteCategoryRejectsLinkedServicesBeforeDeletingOrAuditing() {
        ServiceCategoryCatalog category = mock(ServiceCategoryCatalog.class);
        when(category.getGameCode()).thenReturn("CODMW4");
        when(category.getCode()).thenReturn("CAMO");
        when(categories.findById(9L)).thenReturn(Optional.of(category));
        when(services.countByGameCodeAndCategory("CODMW4", "CAMO")).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.deleteCategory(9L, "admin", null));

        verify(categories, never()).delete(any());
        verifyNoInteractions(audit);
    }

    @Test
    void deleteGameWithoutDependenciesDeletesLinksThenGameAndAudits() {
        GameCatalog game = mock(GameCatalog.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(game.getCode()).thenReturn("CODMW4");
        when(games.findById("CODMW4")).thenReturn(Optional.of(game));
        when(categories.countByGameCode("CODMW4")).thenReturn(0L);
        when(services.countByGameCode("CODMW4")).thenReturn(0L);
        when(gamePlatforms.findByGameCodeOrderBySortOrderAscIdAsc("CODMW4")).thenReturn(List.of());

        service.deleteGame("codmw4", "admin", request);

        InOrder order = inOrder(gamePlatforms, games, audit);
        order.verify(gamePlatforms).deleteByGameCode("CODMW4");
        order.verify(games).delete(game);
        order.verify(audit).log(
                eq("GAME_DELETE"),
                eq("GAME"),
                eq("CODMW4"),
                eq("admin"),
                any(ApiDtos.GameOptionResponse.class),
                eq(null),
                eq("永久删除无依赖游戏"),
                eq(request));
    }
}
