package com.xinghuo.account.service;

import com.xinghuo.account.model.AccountAudit;
import com.xinghuo.account.repo.AccountAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 账号审计。
 *
 * <p>所有审计写入都必须经过这里，由它负责脱敏 —— 手机号、邮箱、微信号不能进日志。
 * 脱敏是「双保险」：既要求调用方不传明文，这里也会再扫一遍，
 * 因为审计日志往往是在排障时被翻出来的，明文一旦落库就很难收回。
 */
@Service
public class AccountAuditService {

    private static final Logger log = LoggerFactory.getLogger(AccountAuditService.class);

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE =
            Pattern.compile("\\b1[3-9]\\d{9}\\b");
    /**
     * QQ 号必须带上下文才脱敏。
     *
     * <p>原先用 {@code \b[1-9]\d{5,11}\b} 无差别匹配任意 6–12 位数字，
     * 结果把 {@code id=1000000}、{@code orderNo=2026100112} 这类**排障与追责
     * 最需要**的标识也抹成了 {@code <id>} —— 脱敏反而破坏了审计证据。
     * 现在只匹配带 {@code qq=} / {@code contact=} 前缀的值。
     */
    private static final Pattern QQ_LIKE =
            Pattern.compile("(?i)(qq[=:：]\\s*)[1-9]\\d{4,11}");
    private static final Pattern CONTACT_LIKE =
            Pattern.compile("(?i)(contact[=:：]\\s*)\\S+");

    private final AccountAuditRepository audits;

    public AccountAuditService(AccountAuditRepository audits) {
        this.audits = audits;
    }

    public void record(Long userId, String action, String detail) {
        record(userId, action, detail, null);
    }

    public void record(Long userId, String action, String detail, HttpServletRequest request) {
        try {
            AccountAudit row = AccountAudit.builder()
                    .userId(userId)
                    .action(action)
                    .detail(scrub(detail))
                    .ip(RequestUtils.clientIp(request))
                    .userAgent(RequestUtils.userAgent(request))
                    .build();
            audits.save(row);
        } catch (RuntimeException e) {
            // 审计失败绝不能让业务流程失败（例如登录已经成功）。
            log.warn("账号审计写入失败 action={} userId={}", action, userId, e);
        }
    }

    /** 用户安全中心使用的最近活动，只允许读取当前账号自己的记录。 */
    public List<AccountAudit> recentForUser(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return audits.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit));
    }

    /**
     * 兜底脱敏：把看起来像邮箱、手机号、QQ 号的片段替换掉。
     *
     * <p>顺序很重要 —— 先处理邮箱（其中可能含数字），再处理手机号（更长），
     * 最后才是 QQ 号，避免手机号被 QQ 规则截断成两段。
     */
    static String scrub(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        String s = EMAIL.matcher(detail).replaceAll("<email>");
        s = PHONE.matcher(s).replaceAll("<phone>");
        s = QQ_LIKE.matcher(s).replaceAll("$1<qq>");
        s = CONTACT_LIKE.matcher(s).replaceAll("$1<contact>");
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
