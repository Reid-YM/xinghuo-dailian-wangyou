package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.ServiceItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** V3D：服务正式上架前的无状态检查，不新增审批表或第二份服务数据源。 */
@Service
public class XingweiServicePublishReviewService {
    private static final String BLOCKER = "BLOCKER";
    private static final String WARNING = "WARNING";

    private final ServiceItemRepository services;
    private final TaxonomyService taxonomy;

    public XingweiServicePublishReviewService(ServiceItemRepository services, TaxonomyService taxonomy) {
        this.services = services;
        this.taxonomy = taxonomy;
    }

    @Transactional(readOnly = true)
    public PublishReview review(Long id) {
        ServiceItem item = services.findById(id).orElseThrow(NotFoundException::new);
        Instant now = Instant.now();
        List<PublishCheck> checks = new ArrayList<>();

        boolean taxonomyPublic = taxonomy.publicAllowed(item.getGameCode(), item.getCategory());
        checks.add(check(
                "TAXONOMY_PUBLIC",
                "游戏与分类可公开",
                BLOCKER,
                taxonomyPublic,
                taxonomyPublic
                        ? item.getGameCode() + " / " + item.getCategory() + " 当前可公开"
                        : "游戏或分类已停用/不可公开，请先修复 taxonomy"));

        boolean slugValid = item.getSlug() != null && item.getSlug().matches("[a-z0-9-]{2,100}");
        checks.add(check(
                "SLUG_VALID",
                "服务标识有效",
                BLOCKER,
                slugValid,
                slugValid ? item.getSlug() : "slug 必须是 2-100 位小写字母、数字或连字符"));

        boolean namePresent = hasText(item.getName());
        checks.add(check(
                "NAME_PRESENT",
                "服务名称完整",
                BLOCKER,
                namePresent,
                namePresent ? item.getName().trim() : "服务名称不能为空"));

        boolean descriptionPresent = hasText(item.getShortDesc());
        checks.add(check(
                "DESCRIPTION_PRESENT",
                "服务说明完整",
                BLOCKER,
                descriptionPresent,
                descriptionPresent ? item.getShortDesc().trim() : "简短说明不能为空"));

        boolean durationPresent = hasText(item.getDurationText());
        checks.add(check(
                "DURATION_PRESENT",
                "预计时长完整",
                BLOCKER,
                durationPresent,
                durationPresent ? item.getDurationText().trim() : "预计时长不能为空"));

        boolean priceValid = validPrice(item);
        checks.add(check(
                "PRICE_VALID",
                "价格配置有效",
                BLOCKER,
                priceValid,
                priceValid ? priceSummary(item) : "价格模式与对应金额不匹配或金额非法"));

        boolean notExpired = item.getExpiresAt() == null || item.getExpiresAt().isAfter(now);
        checks.add(check(
                "NOT_EXPIRED",
                "服务未过期",
                BLOCKER,
                notExpired,
                item.getExpiresAt() == null
                        ? "未设置到期时间"
                        : (notExpired ? "到期时间 " + item.getExpiresAt() : "服务已于 " + item.getExpiresAt() + " 到期")));

        boolean coverPresent = hasText(item.getCoverImage());
        checks.add(check(
                "COVER_RECOMMENDED",
                "服务封面",
                WARNING,
                coverPresent,
                coverPresent ? "已配置封面" : "未配置封面；允许上架，但建议在旧后台补齐展示素材"));

        boolean pendingPublish = !item.isActive() || item.getStatus() == ServiceItem.Status.OFFLINE;
        checks.add(check(
                "PUBLISH_STATE",
                "当前发布状态",
                WARNING,
                pendingPublish,
                pendingPublish
                        ? "当前为离线/停用状态，适合执行发布检查"
                        : "当前已启用，无需重复执行上架"));

        long blockers = checks.stream()
                .filter(check -> BLOCKER.equals(check.severity()) && !check.passed())
                .count();
        long warnings = checks.stream()
                .filter(check -> WARNING.equals(check.severity()) && !check.passed())
                .count();

        return new PublishReview(
                item.getId(),
                item.getSlug(),
                item.getName(),
                blockers == 0,
                blockers,
                warnings,
                checks);
    }

    @Transactional(readOnly = true)
    public void requireReady(Long id) {
        PublishReview review = review(id);
        if (review.ready()) {
            return;
        }
        String failed = review.checks().stream()
                .filter(check -> BLOCKER.equals(check.severity()) && !check.passed())
                .map(PublishCheck::label)
                .toList()
                .toString();
        throw new BadRequestException("上架前检查未通过：" + failed);
    }

    private PublishCheck check(String code, String label, String severity, boolean passed, String detail) {
        return new PublishCheck(code, label, severity, passed, detail);
    }

    private boolean validPrice(ServiceItem item) {
        if (item.getPriceMode() == null) {
            return false;
        }
        return switch (item.getPriceMode()) {
            case QUOTE -> true;
            case FIXED -> nonNegative(item.getPriceFixed());
            case RANGE -> nonNegative(item.getPriceMin())
                    && nonNegative(item.getPriceMax())
                    && item.getPriceMin().compareTo(item.getPriceMax()) <= 0;
        };
    }

    private String priceSummary(ServiceItem item) {
        return switch (item.getPriceMode()) {
            case QUOTE -> "询价模式";
            case FIXED -> "固定价 ¥" + item.getPriceFixed();
            case RANGE -> "区间价 ¥" + item.getPriceMin() + " - ¥" + item.getPriceMax();
        };
    }

    private boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record PublishReview(
            Long serviceId,
            String slug,
            String name,
            boolean ready,
            long blockers,
            long warnings,
            List<PublishCheck> checks) {
    }

    public record PublishCheck(
            String code,
            String label,
            String severity,
            boolean passed,
            String detail) {
        public PublishCheck {
            severity = severity == null ? "" : severity.toUpperCase(Locale.ROOT);
        }
    }
}
