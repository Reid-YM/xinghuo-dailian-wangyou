package com.xinghuo.taxonomy.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCatalogRepository extends JpaRepository<GameCatalog, String> {
    List<GameCatalog> findAllByOrderBySortOrderAscCodeAsc();

    Optional<GameCatalog> findBySeoSlug(String seoSlug);
}
