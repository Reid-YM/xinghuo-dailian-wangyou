/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.GameStatus
 *  com.xinghuo.repository.GameStatusRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.GameStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatusRepository
extends JpaRepository<GameStatus, String> {
    public List<GameStatus> findAllByOrderByGameCodeAsc();
}

