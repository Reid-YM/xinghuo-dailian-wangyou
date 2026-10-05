package com.xinghuo.account.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批量取账号昵称。
 *
 * <p>用 JdbcTemplate 而不是实体查询：评论列表只需要「id → 昵称」这一列，
 * 加载整行实体（含密码哈希等字段）既浪费也没必要。
 *
 * <p>刻意不返回邮箱或联系方式 —— 昵称是唯一可以对外展示的账号字段。
 */
@Service
public class AccountNameLookup {

    private final JdbcTemplate jdbc;

    public AccountNameLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 一次查询取回全部昵称，避免在评论列表里按行查询（N+1）。 */
    @Transactional(readOnly = true)
    public Map<Long, String> names(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream().distinct().toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        Map<Long, String> out = new LinkedHashMap<>();
        jdbc.query("SELECT id, display_name FROM customer_accounts WHERE id IN (" + placeholders + ")",
                rs -> {
                    out.put(rs.getLong("id"), rs.getString("display_name"));
                },
                ids.toArray());
        return out;
    }

    @Transactional(readOnly = true)
    public String name(Long userId) {
        if (userId == null) {
            return null;
        }
        return names(List.of(userId)).get(userId);
    }
}
