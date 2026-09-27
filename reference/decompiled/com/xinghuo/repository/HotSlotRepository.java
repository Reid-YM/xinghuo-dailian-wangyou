/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.HotSlot
 *  com.xinghuo.repository.HotSlotRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.HotSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotSlotRepository
extends JpaRepository<HotSlot, Long> {
    public Optional<HotSlot> findBySlotNo(int var1);

    public List<HotSlot> findAllByOrderBySlotNoAsc();
}

