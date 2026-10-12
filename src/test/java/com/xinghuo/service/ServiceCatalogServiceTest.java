package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.ServiceItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServiceCatalogServiceTest {

    @Test
    void fixedPriceWithoutValueFailsBeforeRepositoryLookup() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        ServiceCatalogService service = new ServiceCatalogService(
                repo, mock(AuditService.class), mock(TaxonomyService.class));

        assertThrows(BadRequestException.class, () -> service.create(
                upsert("fixed-service", "FIXED", null, null, null),
                "admin",
                mock(HttpServletRequest.class)));

        verify(repo, never()).findBySlug(anyString());
        verify(repo, never()).save(any(ServiceItem.class));
    }

    @Test
    void duplicateSlugStillFailsBeforeTaxonomyValidation() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService service = new ServiceCatalogService(repo, mock(AuditService.class), taxonomy);
        when(repo.findBySlug("duplicate-service"))
                .thenReturn(Optional.of(serviceItem(5L, "duplicate-service", ServiceItem.Status.AVAILABLE, true, null)));

        assertThrows(ConflictException.class, () -> service.create(
                upsert("duplicate-service", "FIXED", new BigDecimal("88.00"), null, null),
                "admin",
                mock(HttpServletRequest.class)));

        verify(taxonomy, never()).normalizeGame(anyString());
        verify(repo, never()).save(any(ServiceItem.class));
    }

    @Test
    void publicBySlugRejectsExpiredOrTaxonomyHiddenService() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        ServiceCatalogService service = new ServiceCatalogService(repo, mock(AuditService.class), taxonomy);

        ServiceItem expired = serviceItem(
                10L,
                "expired-service",
                ServiceItem.Status.AVAILABLE,
                true,
                Instant.now().minusSeconds(60));
        when(repo.findBySlug("expired-service")).thenReturn(Optional.of(expired));
        assertThrows(NotFoundException.class, () -> service.publicBySlug("expired-service"));
        verify(taxonomy, never()).publicAllowed("CODMW4", "WEAPON");

        ServiceItem hidden = serviceItem(11L, "hidden-service", ServiceItem.Status.AVAILABLE, true, null);
        when(repo.findBySlug("hidden-service")).thenReturn(Optional.of(hidden));
        when(taxonomy.publicAllowed("CODMW4", "WEAPON")).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.publicBySlug("hidden-service"));
    }

    @Test
    void restoreValidatesTaxonomyThenReactivatesOfflineService() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        AuditService audit = mock(AuditService.class);
        ServiceCatalogService service = new ServiceCatalogService(repo, audit, taxonomy);
        ServiceItem offline = serviceItem(21L, "offline-service", ServiceItem.Status.OFFLINE, false, null);
        when(repo.findById(21L)).thenReturn(Optional.of(offline));

        ApiDtos.AdminServiceResponse response = service.restore(
                21L,
                "admin",
                mock(HttpServletRequest.class));

        verify(taxonomy).validate("CODMW4", "WEAPON");
        assertTrue(offline.isActive());
        assertEquals(ServiceItem.Status.AVAILABLE, offline.getStatus());
        assertTrue(response.active());
        assertEquals("AVAILABLE", response.status());
    }

    private ApiDtos.ServiceUpsert upsert(
            String slug,
            String priceMode,
            BigDecimal priceFixed,
            BigDecimal priceMin,
            BigDecimal priceMax) {
        return new ApiDtos.ServiceUpsert(
                "CODMW4",
                slug,
                "测试服务",
                "WEAPON",
                "测试描述",
                priceMode,
                priceFixed,
                priceMin,
                priceMax,
                "1小时",
                "AVAILABLE",
                null,
                null,
                null,
                null,
                true,
                10,
                "测试变更");
    }

    private ServiceItem serviceItem(
            Long id,
            String slug,
            ServiceItem.Status status,
            boolean active,
            Instant expiresAt) {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        return ServiceItem.builder()
                .id(id)
                .slug(slug)
                .gameCode("CODMW4")
                .name("测试服务")
                .category("WEAPON")
                .shortDesc("测试描述")
                .priceMode(ServiceItem.PriceMode.FIXED)
                .priceFixed(new BigDecimal("88.00"))
                .durationText("1小时")
                .status(status)
                .hotScore(BigDecimal.ZERO)
                .active(active)
                .sortOrder(10)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();
    }
}
