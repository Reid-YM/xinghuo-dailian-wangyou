package com.xinghuo.gamestatus.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatusRepository extends JpaRepository<GameStatus, String> {
    List<GameStatus> findAllByOrderByGameCodeAsc();
}
