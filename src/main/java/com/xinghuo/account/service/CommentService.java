package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.Comment;
import com.xinghuo.account.model.CommentReport;
import com.xinghuo.account.repo.CommentReportRepository;
import com.xinghuo.account.repo.CommentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论。
 *
 * <p>三道反垃圾：频率限制、新用户首条进审核、可配置的屏蔽词。
 * 报告机制单独一层，因为前三道都拦不住「内容本身合规但令人不适」的情况。
 */
@Service
public class CommentService {

    private static final int MAX_LENGTH = 2000;
    private static final int MAX_PER_MINUTE = 1;
    private static final int MAX_PER_DAY = 20;
    private static final int MAX_PAGE_SIZE = 50;

    /** 注册未满该时长的账号，首条评论进入审核。 */
    private static final Duration NEW_ACCOUNT_WINDOW = Duration.ofHours(24);

    private final CommentRepository comments;
    private final CommentReportRepository reports;
    private final RateLimiter rateLimiter;
    private final AccountAuditService audit;
    private final Set<String> blockedWords;
    private final JdbcTemplate jdbc;

    public CommentService(CommentRepository comments,
                          CommentReportRepository reports,
                          RateLimiter rateLimiter,
                          AccountAuditService audit,
                          JdbcTemplate jdbc,
                          @Value("${account.comment.blocked-words:}") String blockedWordsConfig) {
        this.comments = comments;
        this.reports = reports;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.jdbc = jdbc;
        // 屏蔽词表由运维通过配置维护，不硬编码在代码里 —— 硬编码的词表
        // 既跟不上实际滥用方式的变化，也会让改词表变成一次发版。
        this.blockedWords = parseWords(blockedWordsConfig);
    }

    // ------------------------------------------------------------------ 读取

    @Transactional(readOnly = true)
    public List<Comment> listPublished(Comment.TargetType targetType, String targetId,
                                       int page, int size) {
        return comments.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(
                targetType, targetId, Comment.CommentStatus.PUBLISHED,
                PageRequest.of(Math.max(0, page), clampSize(size)));
    }

    @Transactional(readOnly = true)
    public long countPublished(Comment.TargetType targetType, String targetId) {
        return comments.countByTargetTypeAndTargetIdAndStatus(
                targetType, targetId, Comment.CommentStatus.PUBLISHED);
    }

    /** 当前用户在某个目标下的评论（含待审与已驳回）。 */
    @Transactional(readOnly = true)
    public List<Comment> listMineForTarget(Long userId, Comment.TargetType targetType,
                                           String targetId) {
        return comments.findByUserIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(
                userId, targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<Comment> listMine(Long userId, int page, int size) {
        return comments.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(0, page), clampSize(size)));
    }

    // ------------------------------------------------------------------ 发表

    @Transactional
    public Comment create(Account account, Comment.TargetType targetType, String targetId,
                          Long parentId, String rawContent, jakarta.servlet.http.HttpServletRequest request) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw AccountException.badRequest("EMPTY_COMMENT", "评论内容不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            throw AccountException.badRequest("COMMENT_TOO_LONG",
                    "评论最多 " + MAX_LENGTH + " 字");
        }
        if (containsBlockedWord(content)) {
            throw AccountException.badRequest("COMMENT_BLOCKED", "评论包含不被允许的内容");
        }

        // D-3：targetType/targetId 由客户端自由给定，若不校验资源存在性，
        // 评论可挂到任意不存在的资源（或已删除/未发布内容）下，被人当「好评」误导。
        // 只在发表时校验，不影响既有评论的读取。
        if (!targetExists(targetType, targetId)) {
            throw AccountException.notFound("COMMENT_TARGET_NOT_FOUND", "评论的目标内容不存在");
        }

        rateLimiter.check("comment:min:" + account.getId(), MAX_PER_MINUTE, Duration.ofMinutes(1));
        rateLimiter.check("comment:day:" + account.getId(), MAX_PER_DAY, Duration.ofDays(1));

        Comment parent = null;
        if (parentId != null) {
            parent = comments.findById(parentId)
                    .filter(c -> c.isVisible())
                    .orElseThrow(() -> AccountException.notFound("PARENT_NOT_FOUND", "回复的评论不存在"));
            // 回复必须落在同一目标下，否则可以把一条评论挂到毫不相关的页面上。
            if (parent.getTargetType() != targetType || !parent.getTargetId().equals(targetId)) {
                throw AccountException.badRequest("PARENT_MISMATCH", "回复目标不一致");
            }
            // 只做一级：回复的回复仍挂到顶层父评论，避免无限嵌套。
            if (parent.getParentId() != null) {
                parent = comments.findById(parent.getParentId()).orElse(parent);
            }
        }

        boolean newAccount = account.getCreatedAt() != null
                && account.getCreatedAt().isAfter(Instant.now().minus(NEW_ACCOUNT_WINDOW));

        Comment comment = Comment.builder()
                .userId(account.getId())
                .targetType(targetType)
                .targetId(targetId)
                .parentId(parent == null ? null : parent.getId())
                .content(content)
                .status(newAccount ? Comment.CommentStatus.PENDING : Comment.CommentStatus.PUBLISHED)
                .build();
        comment = comments.save(comment);
        audit.record(account.getId(), "COMMENT_CREATE",
                "target=" + targetType + ":" + targetId + " status=" + comment.getStatus(), request);
        return comment;
    }

    /** 用户撤回自己的评论（软删，保留痕迹用于审计）。 */
    @Transactional
    public void deleteOwn(Long userId, Long commentId) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> AccountException.notFound("COMMENT_NOT_FOUND", "评论不存在"));
        if (!comment.getUserId().equals(userId)) {
            // 不区分「不存在」与「不是你的」，避免被用来探测评论归属。
            throw AccountException.notFound("COMMENT_NOT_FOUND", "评论不存在");
        }
        comment.setStatus(Comment.CommentStatus.DELETED);
        comments.save(comment);
    }

    // ------------------------------------------------------------------ 举报

    @Transactional
    public void report(Account account, Long commentId, String reason) {
        Comment comment = comments.findById(commentId)
                .filter(Comment::isVisible)
                .orElseThrow(() -> AccountException.notFound("COMMENT_NOT_FOUND", "评论不存在"));
        if (comment.getUserId().equals(account.getId())) {
            throw AccountException.badRequest("REPORT_OWN", "不能举报自己的评论");
        }
        if (reports.existsByCommentIdAndUserId(commentId, account.getId())) {
            throw AccountException.conflict("ALREADY_REPORTED", "你已经举报过这条评论");
        }
        reports.save(CommentReport.builder()
                .commentId(commentId)
                .userId(account.getId())
                .reason(reason == null ? null : reason.trim())
                .build());
        comments.incrementReportCount(commentId);
        audit.record(account.getId(), "COMMENT_REPORT", "commentId=" + commentId, null);
    }

    // ------------------------------------------------------------ 后台审核

    @Transactional(readOnly = true)
    public List<Comment> listByStatus(Comment.CommentStatus status, int page, int size) {
        return comments.findByStatusOrderByCreatedAtAsc(status,
                PageRequest.of(Math.max(0, page), clampSize(size)));
    }

    @Transactional
    public Comment review(String reviewer, Long commentId, boolean approve, String note) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> AccountException.notFound("COMMENT_NOT_FOUND", "评论不存在"));
        comment.setStatus(approve ? Comment.CommentStatus.PUBLISHED : Comment.CommentStatus.REJECTED);
        comment.setAuditNote(note == null ? null : note.trim());
        comment.setReviewedByName(reviewer);
        comment.setReviewedAt(Instant.now());
        return comments.save(comment);
    }

    @Transactional
    public void adminDelete(String reviewer, Long commentId, String note) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> AccountException.notFound("COMMENT_NOT_FOUND", "评论不存在"));
        comment.setStatus(Comment.CommentStatus.DELETED);
        comment.setAuditNote(note == null ? null : note.trim());
        comment.setReviewedByName(reviewer);
        comment.setReviewedAt(Instant.now());
        comments.save(comment);
    }

    // ------------------------------------------------------------------ 工具

    /** 目标类型的解析，兼容前端小写。 */
    public static Comment.TargetType parseTargetType(String raw) {
        try {
            return Comment.TargetType.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AccountException.badRequest("INVALID_TARGET_TYPE", "不支持的评论目标类型");
        }
    }

    /**
     * 校验评论目标资源是否存在（D-3）。
     *
     * <p>与 {@code V37__user_community.sql} 的口径一致：{@code target_id} 对应
     * {@code game_guides.slug} / {@code game_catalog.code} / {@code services.id}。
     * 攻略只认已发布，服务按 {@code services.is_active=1} 判活 —— 均不暴露被删除
     * 或未发布的资源。
     *
     * <p>用 JdbcTemplate 直查基线共享表（与本模块 {@code OrderLinkService} 的做法一致），
     * 不依赖基线实体，保证能脱离基线独立编译。
     */
    private boolean targetExists(Comment.TargetType targetType, String targetId) {
        String key = targetId == null ? "" : targetId.trim();
        if (key.isEmpty()) {
            return false;
        }
        switch (targetType) {
            case GUIDE:
                return exists("SELECT 1 FROM game_guides WHERE slug = ? AND status = 'PUBLISHED' LIMIT 1", key);
            case GAME:
                return exists("SELECT 1 FROM game_catalog WHERE code = ? LIMIT 1", key);
            case SERVICE:
                // target_id 存的是 services.id（数字字符串）。防手工注入/溢出：
                // 非数字或超出 BIGINT 长度直接不通过。
                if (key.length() > 18 || !key.chars().allMatch(Character::isDigit)) {
                    return false;
                }
                long id = Long.parseLong(key);
                return exists("SELECT 1 FROM services WHERE id = ? AND is_active = 1 LIMIT 1", id);
            default:
                return false;
        }
    }

    private boolean exists(String sql, Object... args) {
        java.util.List<Integer> rows =
                jdbc.query(sql, (rs, i) -> Integer.valueOf(1), args);
        return !rows.isEmpty();
    }

    private boolean containsBlockedWord(String content) {
        if (blockedWords.isEmpty()) {
            return false;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        return blockedWords.stream().anyMatch(lower::contains);
    }

    private static Set<String> parseWords(String config) {
        if (config == null || config.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}
