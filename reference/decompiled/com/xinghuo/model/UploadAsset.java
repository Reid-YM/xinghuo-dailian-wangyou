/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.UploadAsset
 *  com.xinghuo.model.UploadAsset$Kind
 *  com.xinghuo.model.UploadAsset$UploadAssetBuilder
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.UploadAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Generated;

@Entity
@Table(name="upload_assets")
public class UploadAsset {
    @Id
    private UUID id;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=32)
    private Kind kind;
    @Column(name="stored_name", nullable=false, unique=true)
    private String storedName;
    @Column(name="original_name")
    private String originalName;
    @Column(name="content_type", nullable=false, length=64)
    private String contentType;
    @Column(name="byte_size", nullable=false)
    private long byteSize;
    @Column(name="owner_type", length=32)
    private String ownerType;
    @Column(name="owner_ref", length=100)
    private String ownerRef;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Generated
    public static UploadAssetBuilder builder() {
        return new UploadAssetBuilder();
    }

    @Generated
    public UUID getId() {
        return this.id;
    }

    @Generated
    public Kind getKind() {
        return this.kind;
    }

    @Generated
    public String getStoredName() {
        return this.storedName;
    }

    @Generated
    public String getOriginalName() {
        return this.originalName;
    }

    @Generated
    public String getContentType() {
        return this.contentType;
    }

    @Generated
    public long getByteSize() {
        return this.byteSize;
    }

    @Generated
    public String getOwnerType() {
        return this.ownerType;
    }

    @Generated
    public String getOwnerRef() {
        return this.ownerRef;
    }

    @Generated
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public void setId(UUID id) {
        this.id = id;
    }

    @Generated
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    @Generated
    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    @Generated
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    @Generated
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Generated
    public void setByteSize(long byteSize) {
        this.byteSize = byteSize;
    }

    @Generated
    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    @Generated
    public void setOwnerRef(String ownerRef) {
        this.ownerRef = ownerRef;
    }

    @Generated
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public UploadAsset() {
    }

    @Generated
    public UploadAsset(UUID id, Kind kind, String storedName, String originalName, String contentType, long byteSize, String ownerType, String ownerRef, Instant createdAt) {
        this.id = id;
        this.kind = kind;
        this.storedName = storedName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.ownerType = ownerType;
        this.ownerRef = ownerRef;
        this.createdAt = createdAt;
    }
}

