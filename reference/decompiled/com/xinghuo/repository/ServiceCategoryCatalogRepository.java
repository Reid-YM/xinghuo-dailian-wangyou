/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.ServiceCategoryCatalog
 *  com.xinghuo.repository.ServiceCategoryCatalogRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.ServiceCategoryCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryCatalogRepository
extends JpaRepository<ServiceCategoryCatalog, Long> {
    public List<ServiceCategoryCatalog> findAllByOrderBySortOrderAscIdAsc();

    public Optional<ServiceCategoryCatalog> findByGameCodeAndCode(String var1, String var2);

    public long countByGameCode(String var1);
}

