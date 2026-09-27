/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.GameCatalog
 *  com.xinghuo.repository.GameCatalogRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.GameCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCatalogRepository
extends JpaRepository<GameCatalog, String> {
    public List<GameCatalog> findAllByOrderBySortOrderAscCodeAsc();

    public Optional<GameCatalog> findBySeoSlug(String var1);
}

