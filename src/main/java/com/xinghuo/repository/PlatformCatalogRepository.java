package com.xinghuo.repository;

import com.xinghuo.model.PlatformCatalog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformCatalogRepository
extends JpaRepository<PlatformCatalog, String> {
    public List<PlatformCatalog> findAllByOrderBySortOrderAscCodeAsc();
}

