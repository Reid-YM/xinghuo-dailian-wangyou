package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * V3E：在不新增 taxonomy 数据源的前提下，对现有游戏/分类只提供“影响预览 + 启停”。
 * 不创建、不删除、不修改 code，也不开放结构字段编辑。
 */
@Service
public class XingweiTaxonomyControlService {
    private final TaxonomyService taxonomy;
    private final ServiceCatalogService services;

    public XingweiTaxonomyControlService(TaxonomyService taxonomy, ServiceCatalogService services) {
        this.taxonomy = taxonomy;
        this.services = services;
    }

    @Transactional(readOnly = true)
    public TaxonomyImpact gameImpact(String rawCode) {
        ApiDtos.CatalogResponse catalog = taxonomy.catalog(true);
        String code = taxonomy.normalizeGame(rawCode);
        ApiDtos.GameOptionResponse game = catalog.games().stream()
                .filter(row -> row.code().equals(code))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        long activeCategories = catalog.categories().stream()
                .filter(row -> row.gameCode().equals(code) && row.active())
                .count();
        List<ApiDtos.AdminServiceResponse> serviceRows = services.adminList().stream()
                .filter(row -> row.gameCode().equals(code))
                .toList();

        return new TaxonomyImpact(
                "GAME",
                game.code(),
                game.name(),
                game.active(),
                game.categoryCount(),
                activeCategories,
                game.serviceCount(),
                publicCandidateCount(serviceRows),
                game.active()
                        ? "停用后，该游戏及其分类下的公开候选服务会从公开目录隐藏。"
                        : "启用后，仅启用分类下且自身可公开的服务会重新进入公开候选范围。");
    }

    @Transactional(readOnly = true)
    public TaxonomyImpact categoryImpact(Long id) {
        ApiDtos.CatalogResponse catalog = taxonomy.catalog(true);
        ApiDtos.CategoryOptionResponse category = catalog.categories().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        List<ApiDtos.AdminServiceResponse> serviceRows = services.adminList().stream()
                .filter(row -> row.gameCode().equals(category.gameCode()) && row.category().equals(category.code()))
                .toList();

        return new TaxonomyImpact(
                "CATEGORY",
                category.gameCode() + ":" + category.code(),
                category.name(),
                category.active(),
                0L,
                0L,
                category.serviceCount(),
                publicCandidateCount(serviceRows),
                category.active()
                        ? "停用后，该分类下的公开候选服务会从公开目录隐藏。"
                        : "启用后，仅当所属游戏也启用时，该分类下可公开服务才会重新可见。");
    }

    @Transactional
    public ApiDtos.GameOptionResponse setGameActive(
            String rawCode,
            boolean active,
            String reason,
            String operator,
            HttpServletRequest request) {
        TaxonomyImpact impact = gameImpact(rawCode);
        if (impact.active() == active) {
            throw new BadRequestException("游戏状态未变化");
        }
        ApiDtos.CatalogResponse catalog = taxonomy.catalog(true);
        ApiDtos.GameOptionResponse current = catalog.games().stream()
                .filter(row -> row.code().equals(impact.key()))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        ApiDtos.CatalogUpdateRequest update = new ApiDtos.CatalogUpdateRequest(
                current.name(),
                current.iconKey(),
                current.description(),
                current.sortOrder(),
                active,
                normalizedReason(reason));
        ApiDtos.GameOptionResponse changed = taxonomy.updateGame(current.code(), update, operator, request);
        taxonomy.invalidateVisibilityCache();
        return changed;
    }

    @Transactional
    public ApiDtos.CategoryOptionResponse setCategoryActive(
            Long id,
            boolean active,
            String reason,
            String operator,
            HttpServletRequest request) {
        TaxonomyImpact impact = categoryImpact(id);
        if (impact.active() == active) {
            throw new BadRequestException("分类状态未变化");
        }
        ApiDtos.CatalogResponse catalog = taxonomy.catalog(true);
        ApiDtos.CategoryOptionResponse current = catalog.categories().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        ApiDtos.CategoryUpdateRequest update = new ApiDtos.CategoryUpdateRequest(
                current.name(),
                current.iconKey(),
                current.description(),
                current.sortOrder(),
                active,
                normalizedReason(reason));
        ApiDtos.CategoryOptionResponse changed = taxonomy.updateCategory(id, update, operator, request);
        taxonomy.invalidateVisibilityCache();
        return changed;
    }

    private long publicCandidateCount(List<ApiDtos.AdminServiceResponse> rows) {
        Instant now = Instant.now();
        return rows.stream()
                .filter(ApiDtos.AdminServiceResponse::active)
                .filter(row -> !"OFFLINE".equalsIgnoreCase(row.status()))
                .filter(row -> row.expiresAt() == null || row.expiresAt().isAfter(now))
                .count();
    }

    private String normalizedReason(String reason) {
        String value = reason == null ? "" : reason.trim();
        if (value.isEmpty()) {
            throw new BadRequestException("必须填写启停原因");
        }
        if (value.length() > 500) {
            throw new BadRequestException("启停原因不能超过 500 字");
        }
        return value;
    }

    public record TaxonomyImpact(
            String type,
            String key,
            String name,
            boolean active,
            long categoryCount,
            long activeCategoryCount,
            long serviceCount,
            long publicCandidateServiceCount,
            String effect) {
    }
}
