package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.TrackingEvent;
import com.xinghuo.repository.TrackingEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TrackingServiceCharacterizationTest {

    @Test
    void allowedEventsArePersistedWithPayloadAndIpHash() {
        TrackingEventRepository repo = org.mockito.Mockito.mock(TrackingEventRepository.class);
        TrackingService service = new TrackingService(repo);
        List<String> allowed = List.of(
                "hero_quote_click",
                "hero_order_click",
                "service_card_click",
                "quote_started",
                "quote_submit",
                "screenshot_upload",
                "contact_click",
                "order_search",
                "order_search_success",
                "rule_open");

        for (String eventName : allowed) {
            service.track(new ApiDtos.TrackRequest(eventName, "/services/mw4", "camo"), "ip-hash");
        }

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(repo, org.mockito.Mockito.times(allowed.size())).save(captor.capture());
        List<TrackingEvent> saved = captor.getAllValues();
        assertEquals(allowed, saved.stream().map(TrackingEvent::getEventName).toList());
        for (TrackingEvent event : saved) {
            assertEquals("/services/mw4", event.getPath());
            assertEquals("camo", event.getRefValue());
            assertEquals("ip-hash", event.getIpHash());
            assertNotNull(event.getCreatedAt());
        }
    }

    @Test
    void unknownEventIsSilentlyIgnored() {
        TrackingEventRepository repo = org.mockito.Mockito.mock(TrackingEventRepository.class);
        TrackingService service = new TrackingService(repo);

        service.track(new ApiDtos.TrackRequest("unexpected_event", "/", "x"), "ip-hash");

        verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
