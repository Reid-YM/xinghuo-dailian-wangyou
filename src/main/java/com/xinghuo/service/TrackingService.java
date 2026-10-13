package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.TrackingEvent;
import com.xinghuo.repository.TrackingEventRepository;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {
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

    public TrackingService(TrackingEventRepository repo) {
        this.repo = repo;
    }

    public void track(ApiDtos.TrackRequest request, String ipHash) {
        if (!ALLOWED.contains(request.eventName())) {
            return;
        }

        TrackingEvent event = TrackingEvent.builder()
                .eventName(request.eventName())
                .path(request.path())
                .refValue(request.refValue())
                .ipHash(ipHash)
                .createdAt(Instant.now())
                .build();
        this.repo.save(event);
    }
}
