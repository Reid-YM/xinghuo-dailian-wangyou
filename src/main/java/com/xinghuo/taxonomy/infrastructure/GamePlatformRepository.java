package com.xinghuo.taxonomy.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlatformRepository extends JpaRepository<GamePlatform, Long> {
    List<GamePlatform> findAllByOrderByGameCodeAscSortOrderAscIdAsc();

    List<GamePlatform> findByGameCodeOrderBySortOrderAscIdAsc(String gameCode);

    void deleteByGameCode(String gameCode);
}
