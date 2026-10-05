package com.xinghuo.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台「行为埋点」可视化接口。
 *
 * <p>背景：站点一直在用 {@code /api/track} 采集 {@code tracking_events}
 * （询价漏斗：进入询价 → 点联系 → 提交询价 → 查到业务），但后台 10 个面板里
 * 没有任何一个能看到这些数据，等于采了没人看。
 *
 * <p>这里只读聚合，不做任何写入；独立成一个控制器，避免改动体量很大的
 * {@code AdminApiController}。访问受 Spring Security 的
 * {@code /api/admin/**} 规则保护（需 ADMIN / OPERATOR）。
 */
@RestController
@RequestMapping(value={"/api/admin/tracking"})
public class AdminTrackingController {

    /** 与前端漏斗顺序一致：每一步都会计入前面所有步骤的完成数。 */
    private static final String[][] FUNNEL = {
        {"quote_started", "进入询价"},
        {"contact_click", "咨询客服"},
        {"quote_submit", "提交询价"},
        {"order_search_success", "查到业务进度"}
    };

    private final JdbcTemplate jdbc;

    public AdminTrackingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping(value={"/summary"})
    public Map<String, Object> summary(@RequestParam(name="days", defaultValue="7") int days) {
        int window = Math.max(1, Math.min(90, days));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", window);
        out.put("total", scalar("SELECT COUNT(*) FROM tracking_events"));
        out.put("recent", scalar(
            "SELECT COUNT(*) FROM tracking_events WHERE created_at > DATE_SUB(NOW(), INTERVAL ? DAY)", window));
        out.put("lastEventAt", text(
            "SELECT DATE_FORMAT(MAX(created_at), '%Y-%m-%d %H:%i') FROM tracking_events"));

        // 各事件总量 + 窗口内数量
        List<Map<String, Object>> events = new ArrayList<>();
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT event_name, COUNT(*) AS total, "
          + "SUM(created_at > DATE_SUB(NOW(), INTERVAL ? DAY)) AS recent, "
          + "MAX(created_at) AS last_at "
          + "FROM tracking_events GROUP BY event_name ORDER BY total DESC", window);
        for (Map<String, Object> r : rows) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("event", str(r.get("event_name")));
            e.put("total", num(r.get("total")));
            e.put("recent", num(r.get("recent")));
            e.put("lastAt", str(r.get("last_at")));
            events.add(e);
        }
        out.put("events", events);

        // 漏斗（按窗口内计数，缺的补 0）
        Map<String, Long> byName = new LinkedHashMap<>();
        for (Map<String, Object> e : events) {
            byName.put((String) e.get("event"), ((Number) e.get("recent")).longValue());
        }
        List<Map<String, Object>> funnel = new ArrayList<>();
        long first = 0;
        for (String[] step : FUNNEL) {
            long n = byName.getOrDefault(step[0], 0L);
            if (first == 0 && n > 0) {
                first = n;
            }
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("event", step[0]);
            f.put("label", step[1]);
            f.put("count", n);
            f.put("rate", first > 0 ? Math.round(n * 1000.0 / first) / 10.0 : 0.0);
            funnel.add(f);
        }
        out.put("funnel", funnel);

        // 最近每日趋势（窗口内）
        List<Map<String, Object>> trend = new ArrayList<>();
        List<Map<String, Object>> trows = jdbc.queryForList(
            "SELECT DATE_FORMAT(created_at, '%m-%d') AS d, COUNT(*) AS c "
          + "FROM tracking_events WHERE created_at > DATE_SUB(NOW(), INTERVAL ? DAY) "
          + "GROUP BY d ORDER BY d", window);
        for (Map<String, Object> r : trows) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", str(r.get("d")));
            t.put("count", num(r.get("c")));
            trend.add(t);
        }
        out.put("trend", trend);

        return out;
    }

    private long scalar(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    private String text(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, String.class, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}
