package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.*;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.Quote;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MiniWebsiteAdminBridgeServiceTest {
    @Test
    void miniQuoteViewDoesNotExposeCustomerContactOrInternalMetadata() {
        UUID id = UUID.randomUUID();
        ApiDtos.QuoteAdminResponse full = new ApiDtos.QuoteAdminResponse(
                id, "XH-EXAMPLE", "MW4", "迷彩服务", "15%",
                null, "PC", "一周内", "微信", "PRIVATE_WECHAT_IDENTIFIER",
                "SENSITIVE_IP_HASH", "NEW", Instant.parse("2026-11-02T00:00:00Z"));
        MiniWebsiteAdminBridgeService.MiniQuoteView publicView =
                MiniWebsiteAdminBridgeService.MiniQuoteView.from(full);

        assertEquals(id.toString(), publicView.id());
        assertEquals("XH-EXAMPLE", publicView.businessNo());
        assertEquals("NEW", publicView.status());
        assertFalse(publicView.toString().contains("PRIVATE_WECHAT_IDENTIFIER"));
        assertFalse(publicView.toString().contains("SENSITIVE_IP_HASH"));
        assertEquals(9, MiniWebsiteAdminBridgeService.MiniQuoteView.class.getRecordComponents().length);
    }

    @Test
    void onlyHumanReviewedQuotedLeadCanConvert() {
        assertTrue(MiniWebsiteAdminBridgeService.legalConversionStatus(Quote.Status.QUOTED));
        for (Quote.Status status : Quote.Status.values()) {
            if (status != Quote.Status.QUOTED) {
                assertFalse(MiniWebsiteAdminBridgeService.legalConversionStatus(status));
            }
        }
        assertFalse(MiniWebsiteAdminBridgeService.legalConversionStatus(null));
    }

    @Test
    void auditProjectionExcludesSensitiveBeforeAfterAndIp() {
        com.xinghuo.model.AuditLog item = com.xinghuo.model.AuditLog.builder()
                .id(100L).action("QUOTE_CONVERTED").targetType("QUOTE")
                .targetId("XH1234").operatorName("mini:auditor")
                .beforeJson("PRIVATE_BEFORE").afterJson("PRIVATE_AFTER")
                .ipAddress("PRIVATE_IP").reason("PRIVATE_NOTE")
                .createdAt(Instant.parse("2026-11-02T00:00:00Z")).build();
        MiniWebsiteAdminBridgeService.MiniAuditView projected =
                MiniWebsiteAdminBridgeService.MiniAuditView.from(item);
        assertEquals("QUOTE_CONVERTED", projected.action());
        assertEquals(6, MiniWebsiteAdminBridgeService.MiniAuditView.class.getRecordComponents().length);
        assertFalse(projected.toString().contains("PRIVATE_BEFORE"));
        assertFalse(projected.toString().contains("PRIVATE_AFTER"));
        assertFalse(projected.toString().contains("PRIVATE_IP"));
        assertFalse(projected.toString().contains("PRIVATE_NOTE"));
    }

    @Test
    void quoteStatusTransitionFollowsStrictReviewWorkflow() {
        assertTrue(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.NEW, Quote.Status.CONTACTED));
        assertTrue(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.CONTACTED, Quote.Status.QUOTED));
        assertTrue(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.QUOTED, Quote.Status.CLOSED));
        assertTrue(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.CONTACTED, Quote.Status.CANCELLED));
        assertFalse(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.NEW, Quote.Status.QUOTED));
        assertFalse(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.QUOTED, Quote.Status.CONTACTED));
        assertFalse(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.CONVERTED, Quote.Status.CONTACTED));
        assertFalse(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.CANCELLED, Quote.Status.NEW));
        assertFalse(MiniWebsiteAdminBridgeService.legalQuoteTransition(
                Quote.Status.CLOSED, Quote.Status.CLOSED));
        assertFalse(MiniWebsiteAdminBridgeService.legalQuoteTransition(null, Quote.Status.QUOTED));
    }
}
