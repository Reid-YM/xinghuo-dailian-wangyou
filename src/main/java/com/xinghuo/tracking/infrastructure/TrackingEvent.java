package com.xinghuo.tracking.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="tracking_events")
public class TrackingEvent {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="event_name", nullable=false, length=64)
    private String eventName;
    @Column(length=255)
    private String path;
    @Column(name="ref_value", length=255)
    private String refValue;
    @Column(name="ip_hash", length=128)
    private String ipHash;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Generated
    public static TrackingEventBuilder builder() {
        return new TrackingEventBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getEventName() {
        return this.eventName;
    }

    @Generated
    public String getPath() {
        return this.path;
    }

    @Generated
    public String getRefValue() {
        return this.refValue;
    }

    @Generated
    public String getIpHash() {
        return this.ipHash;
    }

    @Generated
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Generated
    public void setPath(String path) {
        this.path = path;
    }

    @Generated
    public void setRefValue(String refValue) {
        this.refValue = refValue;
    }

    @Generated
    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    @Generated
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public TrackingEvent() {
    }

    @Generated
    public TrackingEvent(Long id, String eventName, String path, String refValue, String ipHash, Instant createdAt) {
        this.id = id;
        this.eventName = eventName;
        this.path = path;
        this.refValue = refValue;
        this.ipHash = ipHash;
        this.createdAt = createdAt;
    }

    @Generated
    public static class TrackingEventBuilder {
        @Generated
        private Long id;
        @Generated
        private String eventName;
        @Generated
        private String path;
        @Generated
        private String refValue;
        @Generated
        private String ipHash;
        @Generated
        private Instant createdAt;

        @Generated
        TrackingEventBuilder() {
        }

        @Generated
        public TrackingEventBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public TrackingEventBuilder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        @Generated
        public TrackingEventBuilder path(String path) {
            this.path = path;
            return this;
        }

        @Generated
        public TrackingEventBuilder refValue(String refValue) {
            this.refValue = refValue;
            return this;
        }

        @Generated
        public TrackingEventBuilder ipHash(String ipHash) {
            this.ipHash = ipHash;
            return this;
        }

        @Generated
        public TrackingEventBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public TrackingEvent build() {
            return new TrackingEvent(this.id, this.eventName, this.path, this.refValue, this.ipHash, this.createdAt);
        }

        @Generated
        public String toString() {
            return "TrackingEventBuilder(id=" + this.id + ", eventName=" + this.eventName + ", path=" + this.path + ", refValue=" + this.refValue + ", ipHash=" + this.ipHash + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}

