/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.GamePlatform
 *  com.xinghuo.repository.GamePlatformRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.GamePlatform;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlatformRepository
extends JpaRepository<GamePlatform, Long> {
    public List<GamePlatform> findAllByOrderByGameCodeAscSortOrderAscIdAsc();

    public List<GamePlatform> findByGameCodeOrderBySortOrderAscIdAsc(String var1);

    public void deleteByGameCode(String var1);
}

