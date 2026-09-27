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
        TEST_FEEDBACK_ATTACHMENT
    }
}