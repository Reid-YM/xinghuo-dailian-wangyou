package com.xinghuo.tracking.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    long countByEventName(String eventName);
}
