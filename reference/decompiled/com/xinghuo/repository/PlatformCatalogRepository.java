/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.PlatformCatalog
 *  com.xinghuo.repository.PlatformCatalogRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.PlatformCatalog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformCatalogRepository
extends JpaRepository<PlatformCatalog, String> {
    public List<PlatformCatalog> findAllByOrderBySortOrderAscCodeAsc();
}

