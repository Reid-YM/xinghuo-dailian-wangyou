package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.repo.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录失败记录与账号锁定。
 *
 * <p><b>为什么必须单独一个 Bean 且用 REQUIRES_NEW：</b>
 * 调用方 {@link AccountService#login} 标注了 {@code @Transactional}，而它在密码错误时
 * 会抛出 {@code AccountException}（RuntimeException，Spring 默认对其回滚）。
 * 如果失败计数与审计写在外层事务里，抛出异常时会被一并撤销 ——
 * 实际表现就是：连续输错密码后 {@code failed_attempts} 仍是 0、
 * 账号永远不会被锁定、审计日志里连一条 LOGIN_FAIL 都没有。
 *
 * <p>这个缺陷已由线上实测确认（5 次错误密码后仍能用正确密码登录成功，
 * 且审计日志中只有 REGISTER 与 LOGIN_OK）。修复方式是把失败记录放进
 * 独立事务，让它在外层回滚之前就已提交。
 *
 * <p>注意：Spring 的 {@code REQUIRES_NEW} 依赖代理生效，因此本方法必须由
 * <b>另一个 Bean</b> 调用，不能写在 AccountService 内部自调用。
 */
@Service
public class AccountLockService {

    /** 连续失败达到该次数即锁定。 */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** 锁定时长。 */
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AccountRepository accounts;
    private final AccountAuditService audit;

    public AccountLockService(AccountRepository accounts, AccountAuditService audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    /**
     * 记录一次登录失败。达到阈值时锁定账号。
     *
     * <p>独立事务提交，不受调用方回滚影响。
     *
     * @param accountId 账号 id；账号不存在时传 null（此时只记审计）
     * @param reason    失败原因，用于审计留痕
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long accountId, String reason, HttpServletRequest request) {
        if (accountId == null) {
            // 账号不存在：无计数可加，但失败尝试本身必须留痕，否则无法发现撞库行为。
            audit.record(null, "LOGIN_FAIL", "reason=" + reason, request);
            return;
        }

        Account account = accounts.findById(accountId).orElse(null);
        if (account == null) {
            audit.record(accountId, "LOGIN_FAIL", "reason=" + reason, request);
            return;
        }

        // 只有密码错误才累计计数；账号被停用、已锁定等情况不计入，
        // 否则攻击者可以用错误密码把正常用户永久锁死。
        if (!"BAD_PASSWORD".equals(reason)) {
            audit.record(accountId, "LOGIN_FAIL", "reason=" + reason, request);
            return;
        }

        int fails = account.getFailedAttempts() + 1;
        if (fails >= MAX_FAILED_ATTEMPTS) {
            account.setLockedUntil(Instant.now().plus(LOCK_DURATION));
            account.setFailedAttempts(0);
            accounts.save(account);
            audit.record(accountId, "ACCOUNT_LOCKED", "fails=" + fails, request);
            return;
        }
        account.setFailedAttempts(fails);
        accounts.save(account);
        audit.record(accountId, "LOGIN_FAIL", "reason=BAD_PASSWORD fails=" + fails, request);
    }

    /** 登录成功后清零计数。走调用方事务即可（成功路径不会回滚）。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applySuccess(Account account, String ip) {
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        account.setLastLoginAt(Instant.now());
        account.setLastLoginIp(ip);
        accounts.save(account);
    }
}
