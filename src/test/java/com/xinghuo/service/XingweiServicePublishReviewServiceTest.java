package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.ServiceItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class XingweiServicePublishReviewServiceTest {

    @Test
    void missingCoverIsWarningButDoesNotBlockOtherwiseValidOfflineService() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        XingweiServicePublishReviewService service = new XingweiServicePublishReviewService(repo, taxonomy);
        ServiceItem item = item(11L, ServiceItem.PriceMode.FIXED, new BigDecimal("88.00"), null, null, null);
        when(repo.findById(11L)).thenReturn(Optional.of(item));
        when(taxonomy.publicAllowed("CODMW4", "WEAPON")).thenReturn(true);

        XingweiServicePublishReviewService.PublishReview review = service.review(11L);

        assertTrue(review.ready());
        assertEquals(0L, review.blockers());
        assertEquals(1L, review.warnings());
        assertTrue(review.checks().stream().anyMatch(check ->
                check.code().equals("COVER_RECOMMENDED") && !check.passed() && check.severity().equals("WARNING")));
        service.requireReady(11L);
    }

    @Test
    void expiredAndHiddenTaxonomyBlockPublish() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        XingweiServicePublishReviewService service = new XingweiServicePublishReviewService(repo, taxonomy);
        ServiceItem item = item(
                12L,
                ServiceItem.PriceMode.QUOTE,
                null,
                null,
                null,
                Instant.now().minusSeconds(60));
        when(repo.findById(12L)).thenReturn(Optional.of(item));
        when(taxonomy.publicAllowed("CODMW4", "WEAPON")).thenReturn(false);

        XingweiServicePublishReviewService.PublishReview review = service.review(12L);

        assertFalse(review.ready());
        assertEquals(2L, review.blockers());
        assertThrows(BadRequestException.class, () -> service.requireReady(12L));
    }

    @Test
    void invalidRangePriceIsBlocker() {
        ServiceItemRepository repo = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        XingweiServicePublishReviewService service = new XingweiServicePublishReviewService(repo, taxonomy);
        ServiceItem item = item(
                13L,
                ServiceItem.PriceMode.RANGE,
                null,
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                null);
        when(repo.findById(13L)).thenReturn(Optional.of(item));
        when(taxonomy.publicAllowed("CODMW4", "WEAPON")).thenReturn(true);

        XingweiServicePublishReviewService.PublishReview review = service.review(13L);

        assertFalse(review.ready());
        assertTrue(review.checks().stream().anyMatch(check ->
                check.code().equals("PRICE_VALID") && !check.passed() && check.severity().equals("BLOCKER")));
    }

    private ServiceItem item(
            Long id,
            ServiceItem.PriceMode priceMode,
            BigDecimal fixed,
            BigDecimal min,
            BigDecimal max,
            Instant expiresAt) {
        Instant now = Instant.parse("2026-10-13T00:00:00Z");
        return ServiceItem.builder()
                .id(id)
                .slug("weapon-service")
                .gameCode("CODMW4")
                .name("武器服务")
                .category("WEAPON")
                .shortDesc("手打服务说明")
                .priceMode(priceMode)
                .priceFixed(fixed)
                .priceMin(min)
                .priceMax(max)
                .durationText("1-2 天")
                .status(ServiceItem.Status.OFFLINE)
                .coverImage(null)
                .hotScore(BigDecimal.ZERO)
                .manualPriority(0)
                .expiresAt(expiresAt)
                .active(false)
                .sortOrder(10)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
