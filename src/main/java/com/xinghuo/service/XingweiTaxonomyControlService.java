package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * V3I：taxonomy 启停使用“影响预览 + 前台可见性说明 + 影响快照 token”。
 *
 * <p>新增/资料编辑仍由 V3F 独立接口处理；本服务只负责 active 启停。
 * 真正写入前会重新计算影响，若与用户看到的 impactToken 不一致则拒绝写入，
 * 防止预览之后分类/服务状态已经变化却仍按旧影响执行。
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

        List<ApiDtos.CategoryOptionResponse> categories = catalog.categories().stream()
                .filter(row -> row.gameCode().equals(code))
                .toList();
        Set<String> activeCategoryCodes = categories.stream()
                .filter(ApiDtos.CategoryOptionResponse::active)
                .map(ApiDtos.CategoryOptionResponse::code)
                .collect(Collectors.toSet());
        List<ApiDtos.AdminServiceResponse> serviceRows = services.adminList().stream()
                .filter(row -> row.gameCode().equals(code))
                .toList();
        List<ApiDtos.AdminServiceResponse> candidates = publicCandidateRows(serviceRows);

        long activeCategories = activeCategoryCodes.size();
        long currentVisible = game.active()
                ? candidates.stream().filter(row -> activeCategoryCodes.contains(row.category())).count()
                : 0L;
        long targetVisible = game.active()
                ? 0L
                : candidates.stream().filter(row -> activeCategoryCodes.contains(row.category())).count();

        List<String> warnings = new ArrayList<>();
        String visibilitySummary;
        if (game.active()) {
            visibilitySummary = currentVisible > 0
                    ? "停用后，前台该游戏入口、分类入口以及当前 " + currentVisible + " 个公开服务将全部隐藏。"
                    : "停用后，该游戏会失去前台公开资格；当前没有正在公开的服务。";
            if (currentVisible > 0) warnings.add("将隐藏 " + currentVisible + " 个当前公开服务");
        } else if (activeCategories == 0) {
            visibilitySummary = "启用游戏后，前台仍不会出现可用分类；需要另行启用至少一个分类。";
            warnings.add("当前没有启用分类");
        } else if (targetVisible == 0) {
            visibilitySummary = "启用游戏后游戏/分类可恢复公开资格，但当前没有满足公开条件的服务。";
            warnings.add("当前没有满足公开条件的服务");
        } else {
            visibilitySummary = "启用后，当前 " + activeCategories + " 个启用分类中的 " + targetVisible
                    + " 个服务可重新获得前台公开资格。";
        }

        String token = impactToken(
                "GAME", game.code(), game.active(), game.categoryCount(), activeCategories,
                game.serviceCount(), candidates.size(), currentVisible, targetVisible, true,
                activeCategoryCodes,
                candidates.stream().map(ApiDtos.AdminServiceResponse::id).toList());

        return new TaxonomyImpact(
                "GAME",
                game.code(),
                game.name(),
                game.active(),
                game.categoryCount(),
                activeCategories,
                game.serviceCount(),
                candidates.size(),
                currentVisible,
                targetVisible,
                true,
                visibilitySummary,
                List.copyOf(warnings),
                token,
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
        ApiDtos.GameOptionResponse parent = catalog.games().stream()
                .filter(row -> row.code().equals(category.gameCode()))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        List<ApiDtos.AdminServiceResponse> serviceRows = services.adminList().stream()
                .filter(row -> row.gameCode().equals(category.gameCode()) && row.category().equals(category.code()))
                .toList();
        List<ApiDtos.AdminServiceResponse> candidates = publicCandidateRows(serviceRows);

        long currentVisible = category.active() && parent.active() ? candidates.size() : 0L;
        long targetVisible = !category.active() && parent.active() ? candidates.size() : 0L;
        List<String> warnings = new ArrayList<>();
        String visibilitySummary;
        if (category.active()) {
            visibilitySummary = currentVisible > 0
                    ? "停用后，前台该分类入口以及当前 " + currentVisible + " 个公开服务将隐藏。"
                    : "停用后，该分类会失去前台公开资格；当前没有正在公开的服务。";
            if (currentVisible > 0) warnings.add("将隐藏 " + currentVisible + " 个当前公开服务");
        } else if (!parent.active()) {
            visibilitySummary = "启用分类后前台仍不会显示，因为所属游戏「" + parent.name() + "」当前处于停用状态。";
            warnings.add("所属游戏 " + parent.code() + " 当前停用，需先启用游戏");
        } else if (targetVisible == 0) {
            visibilitySummary = "启用分类后分类可恢复公开资格，但当前没有满足公开条件的服务。";
            warnings.add("当前没有满足公开条件的服务");
        } else {
            visibilitySummary = "启用后，该分类下当前 " + targetVisible + " 个服务可重新获得前台公开资格。";
        }

        String token = impactToken(
                "CATEGORY", category.gameCode() + ":" + category.code(), category.active(), 0L, 0L,
                category.serviceCount(), candidates.size(), currentVisible, targetVisible, parent.active(),
                Set.of(category.code()),
                candidates.stream().map(ApiDtos.AdminServiceResponse::id).toList());

        return new TaxonomyImpact(
                "CATEGORY",
                category.gameCode() + ":" + category.code(),
                category.name(),
                category.active(),
                0L,
                0L,
                category.serviceCount(),
                candidates.size(),
                currentVisible,
                targetVisible,
                parent.active(),
                visibilitySummary,
                List.copyOf(warnings),
                token,
                category.active()
                        ? "停用后，该分类下的公开候选服务会从公开目录隐藏。"
                        : "启用后，仅当所属游戏也启用时，该分类下可公开服务才会重新可见。");
    }

    @Transactional
    public ApiDtos.GameOptionResponse setGameActive(
            String rawCode,
            boolean active,
            String reason,
            String expectedImpactToken,
            String operator,
            HttpServletRequest request) {
        TaxonomyImpact impact = gameImpact(rawCode);
        requireFreshImpact(expectedImpactToken, impact);
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
            String expectedImpactToken,
            String operator,
            HttpServletRequest request) {
        TaxonomyImpact impact = categoryImpact(id);
        requireFreshImpact(expectedImpactToken, impact);
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

    private void requireFreshImpact(String expected, TaxonomyImpact current) {
        String token = expected == null ? "" : expected.trim();
        if (token.isEmpty() || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                current.impactToken().getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("影响数据已变化，请重新查看影响后再确认");
        }
    }

    private List<ApiDtos.AdminServiceResponse> publicCandidateRows(List<ApiDtos.AdminServiceResponse> rows) {
        Instant now = Instant.now();
        return rows.stream()
                .filter(ApiDtos.AdminServiceResponse::active)
                .filter(row -> !"OFFLINE".equalsIgnoreCase(row.status()))
                .filter(row -> row.expiresAt() == null || row.expiresAt().isAfter(now))
                .toList();
    }

    private String impactToken(
            String type,
            String key,
            boolean active,
            long categoryCount,
            long activeCategoryCount,
            long serviceCount,
            long candidateCount,
            long currentVisible,
            long targetVisible,
            boolean parentActive,
            Set<String> activeCategoryCodes,
            List<Long> candidateServiceIds) {
        List<String> categories = activeCategoryCodes.stream().sorted().toList();
        List<Long> serviceIds = candidateServiceIds.stream().sorted(Comparator.naturalOrder()).toList();
        String canonical = String.join("|",
                type,
                key,
                Boolean.toString(active),
                Long.toString(categoryCount),
                Long.toString(activeCategoryCount),
                Long.toString(serviceCount),
                Long.toString(candidateCount),
                Long.toString(currentVisible),
                Long.toString(targetVisible),
                Boolean.toString(parentActive),
                String.join(",", categories),
                serviceIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
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
            long currentPublicServiceCount,
            long targetPublicServiceCount,
            boolean parentActive,
            String visibilitySummary,
            List<String> warnings,
            String impactToken,
            String effect) {
    }
}
