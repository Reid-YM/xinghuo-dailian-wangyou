/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.HotSlot
 *  com.xinghuo.model.HotSlot$HotSlotBuilder
 *  com.xinghuo.model.HotSlot$Mode
 *  com.xinghuo.model.ServiceItem
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.HotSlot;
import com.xinghuo.model.ServiceItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="hot_slots")
public class HotSlot {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="slot_no", nullable=false, unique=true)
    private int slotNo;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="service_id")
    private ServiceItem service;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=16)
    private Mode mode;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static HotSlotBuilder builder() {
        return new HotSlotBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public int getSlotNo() {
        return this.slotNo;
    }

    @Generated
    public ServiceItem getService() {
        return this.service;
    }

    @Generated
    public Mode getMode() {
        return this.mode;
    }

    @Generated
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setSlotNo(int slotNo) {
        this.slotNo = slotNo;
    }

    @Generated
    public void setService(ServiceItem service) {
        this.service = service;
    }

    @Generated
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Generated
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public HotSlot() {
    }

    @Generated
    public HotSlot(Long id, int slotNo, ServiceItem service, Mode mode, Instant updatedAt) {
        this.id = id;
        this.slotNo = slotNo;
        this.service = service;
        this.mode = mode;
        this.updatedAt = updatedAt;
    }
}

