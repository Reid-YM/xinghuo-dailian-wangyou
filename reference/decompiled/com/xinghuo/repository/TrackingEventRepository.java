/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.TrackingEvent
 *  com.xinghuo.repository.TrackingEventRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository
extends JpaRepository<TrackingEvent, Long> {
    public long countByEventName(String var1);
}

