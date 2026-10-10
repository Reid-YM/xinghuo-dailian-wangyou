package com.xinghuo.taxonomy.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCatalogRepository
extends JpaRepository<GameCatalog, String> {
    public List<GameCatalog> findAllByOrderBySortOrderAscCodeAsc();

    public Optional<GameCatalog> findBySeoSlug(String var1);
}
