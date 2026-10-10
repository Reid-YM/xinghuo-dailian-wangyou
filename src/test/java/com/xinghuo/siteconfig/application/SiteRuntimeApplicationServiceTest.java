package com.xinghuo.siteconfig.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.application.GameStatusCode;
import com.xinghuo.gamestatus.application.GameStatusPort;
import com.xinghuo.gamestatus.application.GameStatusView;
import com.xinghuo.gamestatus.application.UpdateGameStatusCommand;
import com.xinghuo.siteconfig.domain.SiteOperatingStatusCode;
import com.xinghuo.siteconfig.domain.SiteRuntimeState;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SiteRuntimeApplicationServiceTest {

    @Test
    void exposesPersistedRuntimeSwitches() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> state(true, false, SiteOperatingStatusCode.OPEN),
                gameStatuses(null));

        assertTrue(service.quoteEnabled());
        assertFalse(service.orderQueryEnabled());
    }

    @Test
    void readsCurrentStateForEachDecision() {
        AtomicReference<SiteRuntimeState> state = new AtomicReference<>(
                state(false, true, SiteOperatingStatusCode.SUSPENDED));
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(state::get, gameStatuses(null));

        assertFalse(service.quoteEnabled());
        state.set(state(true, false, SiteOperatingStatusCode.OPEN));
        assertTrue(service.quoteEnabled());
        assertFalse(service.orderQueryEnabled());
        assertTrue(service.acceptingOrders());
    }

    @Test
    void mw4OpenOverridesClosedSiteFallback() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> state(true, true, SiteOperatingStatusCode.SUSPENDED),
                gameStatuses(view(GameStatusCode.OPEN, "开放接单", "立即询价")));

        SiteOperatingStatusView status = service.operatingStatus();
        assertEquals(SiteOperatingStatusCode.OPEN, status.status());
        assertEquals("open", status.publicCode());
        assertEquals("开放接单", status.shortText());
        assertEquals("立即询价", status.ctaLabel());
        assertTrue(status.acceptingOrders());
    }

    @Test
    void mapsMw4NonOpenStatusesToLegacyPublicCodes() {
        assertMw4Mapping(GameStatusCode.PAUSED, SiteOperatingStatusCode.SUSPENDED);
        assertMw4Mapping(GameStatusCode.MAINTENANCE, SiteOperatingStatusCode.MAINTENANCE);
        assertMw4Mapping(GameStatusCode.LIMITED, SiteOperatingStatusCode.RESERVATION);
        assertMw4Mapping(GameStatusCode.PREORDER, SiteOperatingStatusCode.RESERVATION);
    }

    @Test
    void fallsBackToSiteStatusCopyAndCtaWhenMw4StatusIsAbsent() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(
                        true,
                        true,
                        SiteOperatingStatusCode.RESERVATION,
                        "站点预约中",
                        "预约咨询"),
                gameStatuses(null));

        SiteOperatingStatusView status = service.operatingStatus();
        assertEquals(SiteOperatingStatusCode.RESERVATION, status.status());
        assertEquals("reservation", status.publicCode());
        assertEquals("站点预约中", status.shortText());
        assertEquals("预约咨询", status.ctaLabel());
        assertFalse(status.acceptingOrders());
    }

    @Test
    void mw4MissingCtaFallsBackToSitePrimaryButtonLabel() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(
                        true,
                        true,
                        SiteOperatingStatusCode.SUSPENDED,
                        "站点暂停",
                        "联系客服"),
                gameStatuses(view(GameStatusCode.MAINTENANCE, "游戏维护中", null)));

        SiteOperatingStatusView status = service.operatingStatus();
        assertEquals("游戏维护中", status.shortText());
        assertEquals("联系客服", status.ctaLabel());
    }

    private static void assertMw4Mapping(GameStatusCode gameStatus, SiteOperatingStatusCode expected) {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> state(true, true, SiteOperatingStatusCode.OPEN),
                gameStatuses(view(gameStatus, gameStatus.name(), null)));

        SiteOperatingStatusView status = service.operatingStatus();
        assertEquals(expected, status.status());
        assertFalse(status.acceptingOrders());
    }

    private static SiteRuntimeState state(
            boolean quoteEnabled,
            boolean orderQueryEnabled,
            SiteOperatingStatusCode status) {
        return new SiteRuntimeState(quoteEnabled, orderQueryEnabled, status, "站点状态", "站点按钮");
    }

    private static GameStatusView view(GameStatusCode status, String publicLabel, String ctaLabel) {
        return new GameStatusView(
                "CODMW4",
                status,
                publicLabel,
                null,
                ctaLabel,
                Instant.EPOCH,
                "test",
                Instant.EPOCH,
                1L);
    }

    private static GameStatusApplicationService gameStatuses(GameStatusView resolved) {
        return new GameStatusApplicationService(new GameStatusPort() {
            @Override
            public GameStatusView require(String gameCode) {
                if (resolved == null) {
                    throw new IllegalStateException("missing test status");
                }
                return resolved;
            }

            @Override
            public GameStatusView resolve(String gameCode, GameStatusView fallback) {
                return resolved == null ? fallback : resolved;
            }

            @Override
            public List<GameStatusView> all() {
                return resolved == null ? List.of() : List.of(resolved);
            }

            @Override
            public GameStatusView update(UpdateGameStatusCommand command) {
                throw new UnsupportedOperationException();
            }
        });
    }
}
