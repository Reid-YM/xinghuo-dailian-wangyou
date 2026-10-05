package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.Submission;
import com.xinghuo.account.repo.SubmissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 攻略投稿与审核。
 *
 * <p>内容安全上采取「不允许内嵌 HTML」而不是「过滤危险标签」：白名单过滤需要
 * 持续跟进新的绕过手法，而 Markdown 本身足以表达攻略需要的排版。
 * 直接拒绝原始 HTML 标签，把攻击面收敛到零。
 *
 * <p>这与站点的 CSP 是两道独立防线：CSP 拦的是脚本执行，这里拦的是内容入库。
 */
@Service
public class SubmissionService {

    private static final int MAX_TITLE = 180;
    private static final int MAX_SUMMARY = 500;
    private static final int MAX_CONTENT = 200_000;
    private static final int MAX_PAGE_SIZE = 50;

    private static final int MAX_PER_DAY = 3;
    private static final int MAX_PENDING = 10;

    /** 形如 <tag 或 </tag 的原始 HTML 标签。 */
    private static final Pattern HTML_TAG = Pattern.compile("<\\s*/?\\s*[a-zA-Z!][^>]*>");

    /** Markdown 链接里指向脚本或内联数据的伪协议。 */
    private static final Pattern DANGEROUS_URL =
            Pattern.compile("\\]\\(\\s*(javascript|data|vbscript)\\s*:", Pattern.CASE_INSENSITIVE);

    private final SubmissionRepository submissions;
    private final RateLimiter rateLimiter;
    private final AccountAuditService audit;

    public SubmissionService(SubmissionRepository submissions,
                             RateLimiter rateLimiter,
                             AccountAuditService audit) {
        this.submissions = submissions;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ 投稿

    @Transactional
    public Submission create(Account account, String gameCode, String title, String summary,
                             String contentMd, String coverImage, HttpServletRequest request) {
        rateLimiter.check("submission:day:" + account.getId(), MAX_PER_DAY, Duration.ofDays(1));

        String code = trimmed(gameCode);
        String cleanTitle = trimmed(title);
        String cleanSummary = summary == null ? null : trimmed(summary);
        String content = contentMd == null ? "" : contentMd;

        if (code.isEmpty() || code.length() > 32) {
            throw AccountException.badRequest("INVALID_GAME", "请选择投稿对应的游戏");
        }
        if (cleanTitle.isEmpty()) {
            throw AccountException.badRequest("INVALID_TITLE", "请填写标题");
        }
        if (cleanTitle.length() > MAX_TITLE) {
            throw AccountException.badRequest("INVALID_TITLE", "标题最多 " + MAX_TITLE + " 字");
        }
        if (cleanSummary != null && cleanSummary.length() > MAX_SUMMARY) {
            throw AccountException.badRequest("INVALID_SUMMARY", "摘要最多 " + MAX_SUMMARY + " 字");
        }
        if (content.isBlank()) {
            throw AccountException.badRequest("EMPTY_CONTENT", "正文不能为空");
        }
        if (content.length() > MAX_CONTENT) {
            throw AccountException.badRequest("CONTENT_TOO_LONG", "正文过长");
        }
        validateMarkdown(content);

        // 只算仍在流程中的投稿（已提交/审核中/已通过）：终态的不应继续占额度，
        // 否则用户投满 10 篇后就永久不能再投，而提示语还在说「请等待审核结果」。
        long pending = submissions.countByUserIdAndStatusIn(account.getId(),
                java.util.List.of(Submission.SubmissionStatus.SUBMITTED,
                        Submission.SubmissionStatus.IN_REVIEW,
                        Submission.SubmissionStatus.APPROVED));
        if (pending >= MAX_PENDING) {
            throw AccountException.badRequest("SUBMISSION_QUOTA_EXCEEDED",
                    "同时进行中的投稿已达上限（" + MAX_PENDING + " 篇），请等待审核结果");
        }

        Submission submission = Submission.builder()
                .userId(account.getId())
                .gameCode(code.toUpperCase(Locale.ROOT))
                .title(cleanTitle)
                .summary(cleanSummary)
                .contentMd(content)
                .coverImage(coverImage == null || coverImage.isBlank() ? null : trimmed(coverImage))
                .status(Submission.SubmissionStatus.SUBMITTED)
                .build();
        submission = submissions.save(submission);
        audit.record(account.getId(), "SUBMISSION_CREATE", "id=" + submission.getId(), request);
        return submission;
    }

    @Transactional(readOnly = true)
    public List<Submission> listMine(Long userId, int page, int size) {
        return submissions.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(Math.max(0, page), clampSize(size)));
    }

    @Transactional(readOnly = true)
    public Submission getOwn(Long userId, Long id) {
        return submissions.findById(id)
                .filter(s -> s.getUserId().equals(userId))
                // 不区分「不存在」与「不是你的」，避免探测他人投稿是否存在。
                .orElseThrow(() -> AccountException.notFound("SUBMISSION_NOT_FOUND", "投稿不存在"));
    }

    /** 后台按 id 读取任意投稿（管理员视角，不做归属过滤）。 */
    @Transactional(readOnly = true)
    public Submission getAny(Long id) {
        return submissions.findById(id)
                .orElseThrow(() -> AccountException.notFound("SUBMISSION_NOT_FOUND", "投稿不存在"));
    }

    @Transactional
    public void withdraw(Long userId, Long id, HttpServletRequest request) {
        Submission submission = getOwn(userId, id);
        if (!submission.isWithdrawable()) {
            throw AccountException.badRequest("NOT_WITHDRAWABLE",
                    "该投稿已进入终态，无法撤回；如需修改请联系客服");
        }
        submission.setStatus(Submission.SubmissionStatus.WITHDRAWN);
        submissions.save(submission);
        audit.record(userId, "SUBMISSION_WITHDRAW", "id=" + id, request);
    }

    // ------------------------------------------------------------ 后台审核

    @Transactional(readOnly = true)
    public List<Submission> listByStatus(Submission.SubmissionStatus status, int page, int size) {
        return submissions.findByStatusOrderByCreatedAtAsc(status,
                PageRequest.of(Math.max(0, page), clampSize(size)));
    }

    @Transactional(readOnly = true)
    public long countByStatus(Submission.SubmissionStatus status) {
        return submissions.countByStatus(status);
    }

    @Transactional
    public Submission review(String reviewer, Long id, Submission.SubmissionStatus target,
                             String note) {
        if (target != Submission.SubmissionStatus.IN_REVIEW
                && target != Submission.SubmissionStatus.APPROVED
                && target != Submission.SubmissionStatus.REJECTED) {
            throw AccountException.badRequest("INVALID_REVIEW_STATE", "不支持的审核状态");
        }
        Submission submission = submissions.findById(id)
                .orElseThrow(() -> AccountException.notFound("SUBMISSION_NOT_FOUND", "投稿不存在"));
        if (submission.getStatus() == Submission.SubmissionStatus.PUBLISHED) {
            throw AccountException.badRequest("ALREADY_PUBLISHED", "已发布的投稿不能再改审核状态");
        }
        submission.setStatus(target);
        submission.setReviewerName(reviewer);
        submission.setReviewNote(note == null ? null : note.trim());
        return submissions.save(submission);
    }

    /** 标记为已发布，并回填对应的攻略 slug。 */
    @Transactional
    public Submission markPublished(String reviewer, Long id, String slug) {
        Submission submission = submissions.findById(id)
                .orElseThrow(() -> AccountException.notFound("SUBMISSION_NOT_FOUND", "投稿不存在"));
        String cleanSlug = trimmed(slug);
        if (cleanSlug.isEmpty() || cleanSlug.length() > 140) {
            throw AccountException.badRequest("INVALID_SLUG", "请填写有效的攻略 slug");
        }
        submission.setStatus(Submission.SubmissionStatus.PUBLISHED);
        submission.setPublishedSlug(cleanSlug);
        submission.setPublishedAt(Instant.now());
        submission.setReviewerName(reviewer);
        return submissions.save(submission);
    }

    // ------------------------------------------------------------------ 校验

    /**
     * 投稿内容校验。
     *
     * <p>拒绝原始 HTML 标签与伪协议链接。这里不做「替换」「清洗」而是直接拒绝：
     * 静默改写用户的正文会让作者看到与提交不符的内容，不如明确告知规则。
     */
    private void validateMarkdown(String content) {
        if (HTML_TAG.matcher(content).find()) {
            throw AccountException.badRequest("HTML_NOT_ALLOWED",
                    "投稿不支持内嵌 HTML 标签，请使用 Markdown 语法");
        }
        if (DANGEROUS_URL.matcher(content).find()) {
            throw AccountException.badRequest("DANGEROUS_LINK",
                    "链接地址不被允许，请使用普通的 http/https 链接");
        }
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}
