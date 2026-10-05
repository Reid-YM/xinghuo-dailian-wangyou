package com.xinghuo.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.ProgressEntry;
import com.xinghuo.account.repo.ProgressRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 攻略进度云同步。
 *
 * <p>冲突策略是「乐观锁 + 明确报错」，不是静默覆盖：同一账号在两台设备上
 * 各改一份进度时，后提交的一侧会拿到 409 与服务端当前版本，由用户决定
 * 保留哪一份。静默 last-write-wins 会让用户无声地丢掉另一台设备上的进度。
 */
@Service
public class ProgressService {

    /** 单个工具的载荷上限。 */
    private static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    /** 单账号工具数量上限。会员体系已下线，统一使用固定配额。 */
    private static final int MAX_TOOLS_HARD_CAP = 20;

    private static final Pattern TOOL_KEY = Pattern.compile("[a-z0-9][a-z0-9_-]{0,39}");

    private final ProgressRepository progress;
    private final ObjectMapper json;

    public ProgressService(ProgressRepository progress, ObjectMapper json) {
        this.progress = progress;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Optional<ProgressEntry> get(Long userId, String toolKey) {
        return progress.findByUserIdAndToolKey(userId, normalizeKey(toolKey));
    }

    @Transactional(readOnly = true)
    public List<ProgressEntry> list(Long userId) {
        return progress.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 写入进度。
     *
     * @param expectedVersion 前端持有的版本号；新建时传 0 或 null
     * @throws AccountException 版本冲突（409）或载荷不合法（400）
     */
    @Transactional
    public ProgressEntry put(Long userId, String toolKey, Integer expectedVersion, String payload) {
        String key = normalizeKey(toolKey);
        validatePayload(payload);

        Optional<ProgressEntry> existing = progress.findByUserIdAndToolKey(userId, key);
        if (existing.isEmpty()) {
            int quota = MAX_TOOLS_HARD_CAP;
            if (progress.countByUserId(userId) >= quota) {
                throw AccountException.badRequest("PROGRESS_QUOTA_EXCEEDED",
                        "同步的工具数量已达上限（" + quota + " 个）");
            }
            ProgressEntry entry = ProgressEntry.builder()
                    .userId(userId)
                    .toolKey(key)
                    .payload(payload)
                    .version(1)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return progress.save(entry);
        }

        ProgressEntry entry = existing.get();
        int current = entry.getVersion();
        if (expectedVersion != null && expectedVersion != current) {
            // 把服务端版本回给前端，让它能提示「另一台设备有更新」。
            throw AccountException.conflict("PROGRESS_VERSION_CONFLICT",
                    "进度已在其他设备更新（服务端版本 " + current + "），请刷新后再提交");
        }
        // 版本条件写进 WHERE 做 CAS：并发提交相同版本时只有一次能改到行，
        // 另一次会拿到 0 行受影响并正确返回 409，而不是静默覆盖。
        int changed = progress.updateIfVersionMatches(
                userId, key, current, payload, Instant.now());
        if (changed != 1) {
            throw AccountException.conflict("PROGRESS_VERSION_CONFLICT",
                    "进度已在其他设备更新，请刷新后再提交");
        }
        return progress.findByUserIdAndToolKey(userId, key).orElse(entry);
    }

    @Transactional
    public void delete(Long userId, String toolKey) {
        progress.findByUserIdAndToolKey(userId, normalizeKey(toolKey))
                .ifPresent(progress::delete);
    }

    private String normalizeKey(String toolKey) {
        String key = toolKey == null ? "" : toolKey.trim().toLowerCase(java.util.Locale.ROOT);
        if (!TOOL_KEY.matcher(key).matches()) {
            throw AccountException.badRequest("INVALID_TOOL_KEY", "工具标识不合法");
        }
        return key;
    }

    private void validatePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw AccountException.badRequest("EMPTY_PAYLOAD", "进度内容为空");
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            throw AccountException.badRequest("PAYLOAD_TOO_LARGE", "进度内容超过 64KB 上限");
        }
        try {
            // 只校验合法性，不解析成对象 —— 载荷结构由各工具自行定义。
            json.readTree(payload);
        } catch (Exception e) {
            throw AccountException.badRequest("INVALID_PAYLOAD", "进度内容不是合法的 JSON");
        }
    }
}
