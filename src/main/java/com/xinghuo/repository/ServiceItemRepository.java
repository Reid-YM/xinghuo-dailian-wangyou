package com.xinghuo.repository;

import com.xinghuo.model.ServiceItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceItemRepository
extends JpaRepository<ServiceItem, Long> {
    public Optional<ServiceItem> findBySlug(String var1);

    public List<ServiceItem> findByGameCodeOrderBySortOrderAscIdAsc(String var1);

    public long countByGameCode(String var1);

    public long countByGameCodeAndCategory(String var1, String var2);

    @Query(value="select s.gameCode, count(s) from ServiceItem s group by s.gameCode")
    public List<Object[]> countGroupedByGame();

    @Query(value="select s.gameCode, s.category, count(s) from ServiceItem s group by s.gameCode, s.category")
    public List<Object[]> countGroupedByGameAndCategory();

    @Query(value="select s from ServiceItem s where s.active=true and s.status<>com.xinghuo.model.ServiceItem.Status.OFFLINE and (s.expiresAt is null or s.expiresAt > :now) order by s.sortOrder asc, s.manualPriority desc, s.hotScore desc")
    public List<ServiceItem> findPublic(Instant var1);
}

