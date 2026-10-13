package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.GameStatus;
import com.xinghuo.repository.GameStatusRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameStatusServiceCharacterizationTest {

    private final GameStatusRepository repo = mock(GameStatusRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final GameStatusService service = new GameStatusService(repo, audit);

    @Test
    void requireNormalizesGameCodeAndResolveBlankReturnsFallbackWithoutLookup() {
        GameStatus status = GameStatus.builder().gameCode("MW4").build();
        when(repo.findById("MW4")).thenReturn(Optional.of(status));

        assertSame(status, service.require("  mw4  "));
        verify(repo).findById("MW4");

        GameStatus fallback = GameStatus.builder().gameCode("FALLBACK").build();
        assertSame(fallback, service.resolve("   ", fallback));
    }

    @Test
    void blankRequireAndMissingGameKeepCurrentExceptionSemantics() {
        assertThrows(BadRequestException.class, () -> service.require("  "));
        when(repo.findById("UNKNOWN")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.require("unknown"));
    }

    @Test
    void invalidStatusIsRejectedBeforeRepositoryMutationOrAudit() {
        assertThrows(BadRequestException.class, () -> service.update(
                "mw4", "not-a-status", "正常接单", null, null, "admin", null));

        verifyNoInteractions(repo, audit);
    }

    @Test
    void updateCreatesStatusMapsLegacyAliasTrimsCopyAndUsesAdminFallback() {
        when(repo.findById("CODMW4")).thenReturn(Optional.empty());
        Instant before = Instant.now();

        GameStatus result = service.update(
                "  codmw4  ",
                "BUSY",
                "  预约接单  ",
                "  当前排队中  ",
                "  立即咨询  ",
                null,
                null);
        Instant after = Instant.now();

        assertEquals("CODMW4", result.getGameCode());
        assertEquals(GameStatus.StatusCode.PREORDER, result.getStatusCode());
        assertEquals("预约接单", result.getPublicLabel());
        assertEquals("当前排队中", result.getPublicDetail());
        assertEquals("立即咨询", result.getCtaLabel());
        assertEquals("admin", result.getUpdatedBy());
        assertNotNull(result.getEffectiveAt());
        assertNotNull(result.getUpdatedAt());
        assertTrue(!result.getEffectiveAt().isBefore(before) && !result.getEffectiveAt().isAfter(after));
        assertEquals(result.getEffectiveAt(), result.getUpdatedAt());

        verify(repo).save(result);
        verify(audit).log(
                "GAME_STATUS_UPDATE",
                "GAME_STATUS",
                "CODMW4",
                null,
                null,
                result,
                "后台更新运营状态",
                null);
    }

    @Test
    void updatingExistingStatusKeepsOptionalCopyWhenNullWasSubmitted() {
        Instant oldTime = Instant.parse("2026-10-01T00:00:00Z");
        GameStatus existing = GameStatus.builder()
                .gameCode("MW4")
                .statusCode(GameStatus.StatusCode.PAUSED)
                .publicLabel("旧状态")
                .publicDetail("保留详情")
                .ctaLabel("保留按钮")
                .effectiveAt(oldTime)
                .updatedAt(oldTime)
                .updatedBy("old-admin")
                .build();
        when(repo.findById("MW4")).thenReturn(Optional.of(existing));

        GameStatus result = service.update(
                "mw4", "OPEN", "  正常接单  ", null, null, "operator-a", null);

        assertSame(existing, result);
        assertEquals(GameStatus.StatusCode.OPEN, result.getStatusCode());
        assertEquals("正常接单", result.getPublicLabel());
        assertEquals("保留详情", result.getPublicDetail());
        assertEquals("保留按钮", result.getCtaLabel());
        assertEquals("operator-a", result.getUpdatedBy());
        verify(repo).save(existing);
        verify(audit).log(
                "GAME_STATUS_UPDATE",
                "GAME_STATUS",
                "MW4",
                "operator-a",
                null,
                existing,
                "后台更新运营状态",
                null);
    }
}
