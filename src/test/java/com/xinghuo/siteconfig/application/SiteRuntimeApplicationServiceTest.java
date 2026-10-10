package com.xinghuo.siteconfig.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.siteconfig.domain.SiteRuntimeState;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SiteRuntimeApplicationServiceTest {

    @Test
    void exposesPersistedRuntimeSwitches() {
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(
                () -> new SiteRuntimeState(true, false));

        assertTrue(service.quoteEnabled());
        assertFalse(service.orderQueryEnabled());
    }

    @Test
    void readsCurrentStateForEachDecision() {
        AtomicReference<SiteRuntimeState> state = new AtomicReference<>(new SiteRuntimeState(false, true));
        SiteRuntimeApplicationService service = new SiteRuntimeApplicationService(state::get);

        assertFalse(service.quoteEnabled());
        state.set(new SiteRuntimeState(true, false));
        assertTrue(service.quoteEnabled());
        assertFalse(service.orderQueryEnabled());
    }
}
