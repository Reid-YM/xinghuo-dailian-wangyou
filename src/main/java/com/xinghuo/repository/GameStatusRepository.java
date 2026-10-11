package com.xinghuo.repository;

import com.xinghuo.model.GameStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatusRepository
extends JpaRepository<GameStatus, String> {
    public List<GameStatus> findAllByOrderByGameCodeAsc();
}

