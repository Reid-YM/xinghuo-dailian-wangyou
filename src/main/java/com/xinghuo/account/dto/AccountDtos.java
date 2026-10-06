package com.xinghuo.account.dto;

import com.xinghuo.account.model.Account;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * 账号模块的请求 / 响应体。
 *
 * <p>刻意与基线 jar 的 {@code com.xinghuo.dto.ApiDtos}、{@code CustomerDtos} 完全解耦：
 * 旧模块的 DTO 会随旧类一起从基线 jar 中删除，新模块不引用它们的任何类型。
 */
public final class AccountDtos {

    private AccountDtos() {
    }

    // ------------------------------------------------------------------ 请求

    public record RegisterRequest(
            @NotBlank @Size(max = 16) String contactType,
            @NotBlank @Size(max = 120) String contactValue,
            @NotBlank @Size(max = 60) String displayName,
            @NotBlank @Size(max = 72) String password,
            @NotBlank @Size(max = 12) String code,
            boolean agree) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 16) String contactType,
            @NotBlank @Size(max = 120) String contactValue,
            @NotBlank @Size(max = 72) String password,
            boolean remember) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String oldPassword,
            @NotBlank @Size(max = 72) String newPassword) {
    }

    public record EmailRequest(@NotBlank @Email @Size(max = 160) String email) {
    }

    public record ResetPasswordRequest(
            @Size(max = 160) String email,
            @Size(max = 12) String code,
            @Size(max = 200) String token,
            @NotBlank @Size(max = 72) String newPassword) {
    }

    public record EmailVerifyCodeRequest(@NotBlank @Size(max = 12) String code) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 60) String displayName,
            @Size(max = 500) String avatarUrl) {
    }

    // ------------------------------------------------------------------ 响应

    /** 当前登录用户的档案。联系方式只返回脱敏值。 */
    public record ProfileResponse(
            String publicUserId,
            String displayName,
            String avatarUrl,
            String contactType,
            String contactMasked,
            boolean contactVerified,
            String email,
            boolean emailVerified,
            String role,
            String levelCode,
            Instant createdAt,
            List<String> entitlements) {

        public static ProfileResponse of(Account a, List<String> entitlements) {
            return new ProfileResponse(
                    a.getPublicUserId(),
                    a.getDisplayName(),
                    a.getAvatarUrl(),
                    a.getContactType() == null ? null : a.getContactType().name(),
                    com.xinghuo.account.service.Masking.contact(a.getContactType(), a.getContactValue()),
                    a.isContactVerified(),
                    a.getEmail(),
                    a.isEmailVerified(),
                    a.getRole(),
                    a.getLevelCode(),
                    a.getCreatedAt(),
                    entitlements == null ? List.of() : entitlements);
        }
    }

    /** 「我的设备」列表项。 */
    public record SessionResponse(
            String id,
            String ip,
            String deviceLabel,
            Instant createdAt,
            Instant lastSeenAt,
            boolean current) {
    }

    /** 最近安全活动。只返回脱敏 IP 与粗粒度设备标签，不暴露审计详情。 */
    public record SecurityEventResponse(
            String action,
            String title,
            String description,
            String ipMasked,
            String deviceLabel,
            Instant createdAt) {
    }

    /** 通用成功响应。 */
    public record OkResponse(boolean ok, String message) {

        public static OkResponse of(String message) {
            return new OkResponse(true, message);
        }
    }

    // ------------------------------------------------------------- 订单归属

    /**
     * 订单条目。
     *
     * <p>没有金额字段：{@code customer_orders} 表本身不存金额（询价与订单都不计价），
     * 前端在字段缺失时会显示占位符，因此这里不编造数据。
     */
    public record OrderItem(
            String businessNo,
            String status,
            String statusText,
            Integer progressPercent,
            String progressText,
            String gameName,
            String serviceName,
            Instant createdAt,
            Instant updatedAt) {

        public static OrderItem of(com.xinghuo.account.service.OrderLinkService.OrderRow row) {
            return new OrderItem(
                    row.businessNo(), row.status(), row.statusText(),
                    row.progressPercent(), row.progressText(),
                    row.gameName(), row.serviceName(), row.createdAt(), row.updatedAt());
        }

        /**
         * 候选列表项：businessNo 脱敏（D-1）。
         *
         * <p>候选是「可能属于你的历史订单」，只做缩小范围用，不该把完整业务编号
         * 一次性列给前端 —— 一份候选截图就会把多笔单号带出，削弱按单号查询的枚举
         * 防护。认领成功后走 {@link #of} 把完整单号给本人。
         */
        public static OrderItem ofMasked(com.xinghuo.account.service.OrderLinkService.OrderRow row) {
            return new OrderItem(
                    com.xinghuo.account.service.Masking.orderNo(row.businessNo()),
                    row.status(), row.statusText(),
                    row.progressPercent(), row.progressText(),
                    row.gameName(), row.serviceName(), row.createdAt(), row.updatedAt());
        }
    }

    public record OrderListResponse(List<OrderItem> items, long total, int page, int size) {
    }

    public record ClaimRequest(@NotBlank @Size(max = 40) String businessNo) {
    }

    // ----------------------------------------------------------- 进度云同步

    /** 进度元信息（列表用，不含载荷本身）。 */
    public record ProgressMeta(String toolKey, int version, Instant updatedAt) {
    }

    public record ProgressResponse(String toolKey, int version, String payload, Instant updatedAt) {
    }

    public record ProgressPutRequest(Integer version, @NotBlank String payload) {
    }

    // --------------------------------------------------------------- 社区互动

    /**
     * 评论条目。
     *
     * <p>只暴露 authorName，不带 userId 与任何联系方式：评论是公开内容，
     * 账号 id 与联系方式不该出现在公开响应里。
     */
    public record CommentItem(
            Long id,
            Long parentId,
            String authorName,
            String content,
            String status,
            Instant createdAt,
            boolean mine) {

        public static CommentItem of(com.xinghuo.account.model.Comment c, String authorName,
                                     Long viewerId) {
            return new CommentItem(
                    c.getId(),
                    c.getParentId(),
                    authorName == null ? "匿名用户" : authorName,
                    c.getContent(),
                    c.getStatus().name(),
                    c.getCreatedAt(),
                    viewerId != null && viewerId.equals(c.getUserId()));
        }
    }

    public record CommentCreateRequest(
            @NotBlank @Size(max = 16) String targetType,
            @NotBlank @Size(max = 64) String targetId,
            Long parentId,
            @NotBlank @Size(max = 2000) String content) {
    }

    public record CommentReportRequest(@Size(max = 120) String reason) {
    }

    public record FavoriteToggleRequest(
            @NotBlank @Size(max = 16) String targetType,
            @NotBlank @Size(max = 64) String targetId) {
    }

    public record FavoriteToggleResponse(boolean favorited, long total) {
    }

    public record FavoriteItem(String targetType, String targetId, Instant createdAt) {
    }

    // --------------------------------------------------------------- 攻略投稿

    public record SubmissionCreateRequest(
            @NotBlank @Size(max = 32) String gameCode,
            @NotBlank @Size(max = 180) String title,
            @Size(max = 500) String summary,
            @NotBlank String contentMd,
            @Size(max = 500) String coverImage) {
    }

    /** 投稿条目。列表里不回传正文，避免一次响应拖回几十万字。 */
    public record SubmissionItem(
            Long id,
            String gameCode,
            String title,
            String summary,
            String status,
            String statusText,
            String reviewNote,
            String publishedSlug,
            Instant createdAt,
            Instant updatedAt) {

        public static SubmissionItem of(com.xinghuo.account.model.Submission s) {
            return new SubmissionItem(
                    s.getId(), s.getGameCode(), s.getTitle(), s.getSummary(),
                    s.getStatus().name(), submissionStatusText(s.getStatus()),
                    s.getReviewNote(), s.getPublishedSlug(), s.getCreatedAt(), s.getUpdatedAt());
        }

        private static String submissionStatusText(
                com.xinghuo.account.model.Submission.SubmissionStatus status) {
            return switch (status) {
                case SUBMITTED -> "已提交，等待审核";
                case IN_REVIEW -> "审核中";
                case APPROVED -> "已通过，等待发布";
                case REJECTED -> "未通过";
                case PUBLISHED -> "已发布";
                case WITHDRAWN -> "已撤回";
            };
        }
    }

    public record SubmissionDetail(
            Long id,
            String gameCode,
            String title,
            String summary,
            String contentMd,
            String coverImage,
            String status,
            String reviewNote,
            String publishedSlug,
            Instant createdAt,
            Instant updatedAt) {

        public static SubmissionDetail of(com.xinghuo.account.model.Submission s) {
            return new SubmissionDetail(
                    s.getId(), s.getGameCode(), s.getTitle(), s.getSummary(), s.getContentMd(),
                    s.getCoverImage(), s.getStatus().name(), s.getReviewNote(),
                    s.getPublishedSlug(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }

}
