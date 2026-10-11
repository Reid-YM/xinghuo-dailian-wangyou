package com.xinghuo.taxonomy.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryCatalogRepository extends JpaRepository<ServiceCategoryCatalog, Long> {
    List<ServiceCategoryCatalog> findAllByOrderBySortOrderAscIdAsc();

    Optional<ServiceCategoryCatalog> findByGameCodeAndCode(String gameCode, String code);

    long countByGameCode(String gameCode);
}
