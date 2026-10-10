package com.xinghuo.tracking.infrastructure;

import com.xinghuo.model.TrackingEvent;
import com.xinghuo.repository.TrackingEventRepository;
import com.xinghuo.tracking.application.TrackingCommand;
import com.xinghuo.tracking.application.TrackingPort;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Tracking-owned allow-list and persistence adapter. */
@Component
public class TrackingPersistenceAdapter implements TrackingPort {
    private static final Set<String> ALLOWED = Set.of(
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

    private final TrackingEventRepository repo;

    public TrackingPersistenceAdapter(TrackingEventRepository repo) {
        this.repo = repo;
    }

    @Override
    public void track(TrackingCommand command) {
        if (!ALLOWED.contains(command.eventName())) {
            return;
        }
        repo.save(TrackingEvent.builder()
                .eventName(command.eventName())
                .path(command.path())
                .refValue(command.refValue())
                .ipHash(command.ipHash())
                .createdAt(Instant.now())
                .build());
    }
}
