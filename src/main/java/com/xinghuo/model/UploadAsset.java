package com.xinghuo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "upload_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadAsset {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "owner_type", length = 32)
    private String ownerType;

    @Column(name = "owner_ref", length = 100)
    private String ownerRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum Kind {
        QUOTE_SCREENSHOT,
        ORDER_PROGRESS,
        SITE_HERO,
        SITE_BACKGROUND,
        SITE_WECHAT_QR,
        SERVICE_COVER,
        GAME_COVER,
        GAME_LOGO,
        TOOL_STATION_COVER,
        USER_AVATAR,
        TEST_FEEDBACK_ATTACHMENT
    }

    @Generated
    public static class UploadAssetBuilder {
        @Generated
        private UUID id;
        @Generated
        private Kind kind;
        @Generated
        private String storedName;
        @Generated
        private String originalName;
        @Generated
        private String contentType;
        @Generated
        private long byteSize;
        @Generated
        private String ownerType;
        @Generated
        private String ownerRef;
        @Generated
        private Instant createdAt;

        @Generated
        UploadAssetBuilder() {
        }

        @Generated
        public UploadAssetBuilder id(UUID uUID) {
            this.id = uUID;
            return this;
        }

        @Generated
        public UploadAssetBuilder kind(Kind kind) {
            this.kind = kind;
            return this;
        }

        @Generated
        public UploadAssetBuilder storedName(String string) {
            this.storedName = string;
            return this;
        }

        @Generated
        public UploadAssetBuilder originalName(String string) {
            this.originalName = string;
            return this;
        }

        @Generated
        public UploadAssetBuilder contentType(String string) {
            this.contentType = string;
            return this;
        }

        @Generated
        public UploadAssetBuilder byteSize(long l) {
            this.byteSize = l;
            return this;
        }

        @Generated
        public UploadAssetBuilder ownerType(String string) {
            this.ownerType = string;
            return this;
        }

        @Generated
        public UploadAssetBuilder ownerRef(String string) {
            this.ownerRef = string;
            return this;
        }

        @Generated
        public UploadAssetBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public UploadAsset build() {
            return new UploadAsset(this.id, this.kind, this.storedName, this.originalName, this.contentType, this.byteSize, this.ownerType, this.ownerRef, this.createdAt);
        }

        @Generated
        public String toString() {
            return "UploadAssetBuilder(id=" + String.valueOf(this.id) + ", kind=" + String.valueOf(this.kind) + ", storedName=" + this.storedName + ", originalName=" + this.originalName + ", contentType=" + this.contentType + ", byteSize=" + this.byteSize + ", ownerType=" + this.ownerType + ", ownerRef=" + this.ownerRef + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}