package com.xinghuo.taxonomy.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryCatalogRepository
extends JpaRepository<ServiceCategoryCatalog, Long> {
    public List<ServiceCategoryCatalog> findAllByOrderBySortOrderAscIdAsc();

    public Optional<ServiceCategoryCatalog> findByGameCodeAndCode(String var1, String var2);

    public long countByGameCode(String var1);
}
