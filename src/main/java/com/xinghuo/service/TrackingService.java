package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.TrackingEvent;
import com.xinghuo.repository.TrackingEventRepository;
import java.time.Instant;
import java.util.Set;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {
    private static final Set<String> ALLOWED = Set.of("hero_quote_click", "hero_order_click", "service_card_click", "quote_started", "quote_submit", "screenshot_upload", "contact_click", "order_search", "order_search_success", "rule_open");
    private final TrackingEventRepository repo;

    public void track(ApiDtos.TrackRequest r, String ipHash) {
        if (!ALLOWED.contains(r.eventName())) {
            return;
        }
        this.repo.save(TrackingEvent.builder().eventName(r.eventName()).path(r.path()).refValue(r.refValue()).ipHash(ipHash).createdAt(Instant.now()).build());
    }

    @Generated
    public TrackingService(TrackingEventRepository repo) {
        this.repo = repo;
    }
}

