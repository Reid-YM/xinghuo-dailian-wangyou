package com.xinghuo.account.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 进程内滑动窗口限流。
 *
 * <p>不引入 Redis：站点是单机部署，蓝绿期间最多两个实例并存，限流精度损失
 * 可以接受。代价是实例重启后计数归零 —— 对「防暴力破解」而言，重启本身
 * 就是运维事件，不是攻击者的常规手段，可接受。
 *
 * <p>内存安全：每个 key 只保留窗口内的命中时间戳，且每次调用有概率触发一次
 * 全量清扫，避免被大量伪造 key（例如伪造 X-Forwarded-For）撑爆内存。
 */
@Component
public class RateLimiter {

    private static final int SWEEP_EVERY = 512;

    /**
     * 键数量上限。
     *
     * <p>键里含请求来源（IP 等），也就是说键空间由外部输入决定。修复前
     * {@code hits} 没有任何上限，配合「X-Forwarded-For 可伪造」，
     * 攻击者每请求一个新 IP 就能永久新增一个 ArrayDeque —— 足以把堆撑爆。
     * 超过上限时先清扫过期窗口，仍然超限就整体清空：宁可短暂放宽限流，
     * 也不能让外部输入决定内存占用。
     */
    private static final int MAX_KEYS = 50_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong calls =
            new java.util.concurrent.atomic.AtomicLong();

    /**
     * 记录一次命中并校验是否超限。
     *
     * @throws AccountException 超限时抛出 429
     */
    public void check(String key, int limit, Duration window) {
        guardCapacity(window);
        maybeSweep(window);
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw AccountException.rateLimited("操作过于频繁，请稍后再试");
            }
            deque.addLast(now);
        }
    }

    /** 只查询不记录，用于「还剩几次」类提示。 */
    public int count(String key, Duration window) {
        Deque<Long> deque = hits.get(key);
        if (deque == null) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - window.toMillis();
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            return deque.size();
        }
    }

    /** 成功后清除计数，例如登录成功就清掉该账号的失败计数。 */
    public void reset(String key) {
        hits.remove(key);
    }

    /** 键空间由输入决定，必须有硬上限。 */
    private void guardCapacity(Duration window) {
        if (hits.size() <= MAX_KEYS) {
            return;
        }
        long cutoff = System.currentTimeMillis() - window.toMillis();
        hits.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
        if (hits.size() > MAX_KEYS) {
            hits.clear();
        }
    }

    private void maybeSweep(Duration window) {
        if (calls.incrementAndGet() % SWEEP_EVERY != 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - window.toMillis();
        hits.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }
}
