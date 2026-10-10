package com.xinghuo.taxonomy.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformCatalogRepository extends JpaRepository<PlatformCatalog, String> {
    List<PlatformCatalog> findAllByOrderBySortOrderAscCodeAsc();
}
