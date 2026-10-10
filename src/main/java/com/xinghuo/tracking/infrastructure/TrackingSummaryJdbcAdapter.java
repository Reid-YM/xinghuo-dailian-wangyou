package com.xinghuo.tracking.infrastructure;

import com.xinghuo.tracking.application.TrackingSummaryPort;
import com.xinghuo.tracking.application.TrackingSummaryView;
import com.xinghuo.tracking.application.TrackingSummaryView.EventSummary;
import com.xinghuo.tracking.application.TrackingSummaryView.FunnelStep;
import com.xinghuo.tracking.application.TrackingSummaryView.TrendPoint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrackingSummaryJdbcAdapter implements TrackingSummaryPort {
    private static final String[][] FUNNEL = {
        {"quote_started", "进入询价"},
        {"contact_click", "咨询客服"},
        {"quote_submit", "提交询价"},
        {"order_search_success", "查到业务进度"}
    };

    private final JdbcTemplate jdbc;

    public TrackingSummaryJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TrackingSummaryView summary(int days) {
        long total = scalar("SELECT COUNT(*) FROM tracking_events");
        long recent = scalar(
                "SELECT COUNT(*) FROM tracking_events WHERE created_at > DATE_SUB(NOW(), INTERVAL ? DAY)",
                days);
        String lastEventAt = text(
                "SELECT DATE_FORMAT(MAX(created_at), '%Y-%m-%d %H:%i') FROM tracking_events");

        List<EventSummary> events = eventSummaries(days);
        List<FunnelStep> funnel = funnel(events);
        List<TrendPoint> trend = trend(days);
        return new TrackingSummaryView(days, total, recent, lastEventAt, events, funnel, trend);
    }

    private List<EventSummary> eventSummaries(int days) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT event_name, COUNT(*) AS total, "
                        + "SUM(created_at > DATE_SUB(NOW(), INTERVAL ? DAY)) AS recent, "
                        + "MAX(created_at) AS last_at "
                        + "FROM tracking_events GROUP BY event_name ORDER BY total DESC",
                days);
        List<EventSummary> events = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            events.add(new EventSummary(
                    str(row.get("event_name")),
                    num(row.get("total")),
                    num(row.get("recent")),
                    str(row.get("last_at"))));
        }
        return events;
    }

    private List<FunnelStep> funnel(List<EventSummary> events) {
        Map<String, Long> byName = new LinkedHashMap<>();
        for (EventSummary event : events) {
            byName.put(event.event(), event.recent());
        }
        List<FunnelStep> funnel = new ArrayList<>();
        long first = 0;
        for (String[] step : FUNNEL) {
            long count = byName.getOrDefault(step[0], 0L);
            if (first == 0 && count > 0) {
                first = count;
            }
            double rate = first > 0 ? Math.round(count * 1000.0 / first) / 10.0 : 0.0;
            funnel.add(new FunnelStep(step[0], step[1], count, rate));
        }
        return funnel;
    }

    private List<TrendPoint> trend(int days) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT DATE_FORMAT(created_at, '%m-%d') AS d, COUNT(*) AS c "
                        + "FROM tracking_events WHERE created_at > DATE_SUB(NOW(), INTERVAL ? DAY) "
                        + "GROUP BY d ORDER BY d",
                days);
        List<TrendPoint> trend = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            trend.add(new TrendPoint(str(row.get("d")), num(row.get("c"))));
        }
        return trend;
    }

    private long scalar(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private String text(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, String.class, args);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long num(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
