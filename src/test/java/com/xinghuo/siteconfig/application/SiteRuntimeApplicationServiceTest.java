package com.xinghuo.siteconfig.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.gamestatus.application.GameStatusCode;
import com.xinghuo.gamestatus.application.GameStatusPort;
import com.xinghuo.gamestatus.application.GameStatusView;
import com.xinghuo.gamestatus.application.UpdateGameStatusCommand;
import com.xinghuo.siteconfig.domain.SiteRuntimeState;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SiteRuntimeApplicationServiceTest {

    @Test
    void exposesPersistedRuntimeSwitches() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(true, false, true),
                gameStatuses(null));

        assertTrue(service.quoteEnabled());
        assertFalse(service.orderQueryEnabled());
    }

    @Test
    void readsCurrentStateForEachDecision() {
        AtomicReference<SiteRuntimeState> state = new AtomicReference<>(new SiteRuntimeState(false, true, false));
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(state::get, gameStatuses(null));

        assertFalse(service.quoteEnabled());
        state.set(new SiteRuntimeState(true, false, true));
        assertTrue(service.quoteEnabled());
        assertFalse(service.orderQueryEnabled());
        assertTrue(service.acceptingOrders());
    }

    @Test
    void mw4OpenOverridesClosedSiteFallback() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(true, true, false),
                gameStatuses(view(GameStatusCode.OPEN)));

        assertTrue(service.acceptingOrders());
    }

    @Test
    void mw4NonOpenStatusesOverrideOpenSiteFallback() {
        for (GameStatusCode status : List.of(
                GameStatusCode.PAUSED,
                GameStatusCode.MAINTENANCE,
                GameStatusCode.LIMITED,
                GameStatusCode.PREORDER)) {
            SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                    () -> new SiteRuntimeState(true, true, true),
                    gameStatuses(view(status)));

            assertFalse(service.acceptingOrders(), status + " must not accept orders");
        }
    }

    @Test
    void fallsBackToSiteStatusWhenMw4StatusIsAbsent() {
        SiteRuntimeApplicationService open = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(true, true, true),
                gameStatuses(null));
        SiteRuntimeApplicationService closed = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(true, true, false),
                gameStatuses(null));

        assertTrue(open.acceptingOrders());
        assertFalse(closed.acceptingOrders());
    }

    private static GameStatusView view(GameStatusCode status) {
        return new GameStatusView(
                "CODMW4",
                status,
                status.name(),
                null,
                null,
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
