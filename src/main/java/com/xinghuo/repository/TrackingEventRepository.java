package com.xinghuo.repository;

import com.xinghuo.model.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository
extends JpaRepository<TrackingEvent, Long> {
    public long countByEventName(String var1);
}

