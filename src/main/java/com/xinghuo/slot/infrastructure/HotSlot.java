package com.xinghuo.slot.infrastructure;

import com.xinghuo.catalog.infrastructure.ServiceItem;
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

    @Generated
    public static class HotSlotBuilder {
        @Generated
        private Long id;
        @Generated
        private int slotNo;
        @Generated
        private ServiceItem service;
        @Generated
        private Mode mode;
        @Generated
        private Instant updatedAt;

        @Generated
        HotSlotBuilder() {
        }

        @Generated
        public HotSlotBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public HotSlotBuilder slotNo(int slotNo) {
            this.slotNo = slotNo;
            return this;
        }

        @Generated
        public HotSlotBuilder service(ServiceItem service) {
            this.service = service;
            return this;
        }

        @Generated
        public HotSlotBuilder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        @Generated
        public HotSlotBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public HotSlot build() {
            return new HotSlot(this.id, this.slotNo, this.service, this.mode, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "HotSlotBuilder(id=" + this.id + ", slotNo=" + this.slotNo + ", service=" + String.valueOf(this.service) + ", mode=" + String.valueOf(this.mode) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }

    public static enum Mode {
        MANUAL,
        AUTO;

    }
}
