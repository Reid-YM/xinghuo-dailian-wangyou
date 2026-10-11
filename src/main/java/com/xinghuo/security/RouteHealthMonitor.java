package com.xinghuo.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RouteHealthMonitor {
    private static final Logger log = LoggerFactory.getLogger(RouteHealthMonitor.class);
    private static final int MAX_KEYS = 500;
    private final ConcurrentHashMap<String, AtomicLong> notFound = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> redirects = new ConcurrentHashMap<>();
    private final AtomicLong notFoundTotal = new AtomicLong();
    private final AtomicLong redirectTotal = new AtomicLong();
    private volatile Instant since = Instant.now();

    public void recordNotFound(String path) {
        this.notFoundTotal.incrementAndGet();
        if (path == null) {
            return;
        }
        AtomicLong counter = this.bump(this.notFound, path);
        if (counter != null && counter.get() == 1L) {
            log.warn("404 first seen path={}", path);
        }
    }

    public void recordRedirect(String from, String to) {
        this.redirectTotal.incrementAndGet();
        if (from == null) {
            return;
        }
        AtomicLong counter = this.bump(this.redirects, from);
        if (counter != null && counter.get() == 1L) {
            log.info("301 first seen from={} to={}", from, to);
        }
    }

    private AtomicLong bump(Map<String, AtomicLong> map, String key) {
        AtomicLong existing = map.get(key);
        if (existing != null) {
            existing.incrementAndGet();
            return existing;
        }
        if (map.size() >= MAX_KEYS) {
            return null;
        }
        AtomicLong created = new AtomicLong(1L);
        AtomicLong prev = map.putIfAbsent(key, created);
        if (prev != null) {
            prev.incrementAndGet();
            return prev;
        }
        return created;
    }

    public Map<String, Object> snapshot(int limit) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("since", this.since);
        out.put("notFoundTotal", this.notFoundTotal.get());
        out.put("redirectTotal", this.redirectTotal.get());
        out.put("notFound", this.topEntries(this.notFound, limit));
        out.put("redirects", this.topEntries(this.redirects, limit));
        return out;
    }

    private List<Map<String, Object>> topEntries(Map<String, AtomicLong> map, int limit) {
        ArrayList<Map.Entry<String, AtomicLong>> rows = new ArrayList<>(map.entrySet());
        rows.sort(Comparator.<Map.Entry<String, AtomicLong>>comparingLong(e -> e.getValue().get()).reversed());
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> entry : rows) {
            if (out.size() >= limit) {
                break;
            }
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("path", entry.getKey());
            row.put("count", entry.getValue().get());
            out.add(row);
        }
        return out;
    }
}
