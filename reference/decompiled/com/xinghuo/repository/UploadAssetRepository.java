/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.UploadAsset
 *  com.xinghuo.repository.UploadAssetRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.UploadAsset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadAssetRepository
extends JpaRepository<UploadAsset, UUID> {
}

