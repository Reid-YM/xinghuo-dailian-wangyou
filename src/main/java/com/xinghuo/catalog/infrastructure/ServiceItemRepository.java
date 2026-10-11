package com.xinghuo.catalog.infrastructure;

import com.xinghuo.model.ServiceItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    Optional<ServiceItem> findBySlug(String slug);

    List<ServiceItem> findByGameCodeOrderBySortOrderAscIdAsc(String gameCode);

    long countByGameCode(String gameCode);

    long countByGameCodeAndCategory(String gameCode, String category);

    @Query("select s.gameCode, count(s) from ServiceItem s group by s.gameCode")
    List<Object[]> countGroupedByGame();

    @Query("select s.gameCode, s.category, count(s) from ServiceItem s group by s.gameCode, s.category")
    List<Object[]> countGroupedByGameAndCategory();

    @Query("select s from ServiceItem s where s.active=true and s.status<>com.xinghuo.model.ServiceItem.Status.OFFLINE and (s.expiresAt is null or s.expiresAt > :now) order by s.sortOrder asc, s.manualPriority desc, s.hotScore desc")
    List<ServiceItem> findPublic(@Param("now") Instant now);
}
