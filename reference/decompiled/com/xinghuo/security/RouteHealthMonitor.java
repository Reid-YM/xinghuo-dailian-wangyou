/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.security.RouteHealthMonitor
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
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
    private final ConcurrentHashMap<String, AtomicLong> notFound = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, AtomicLong> redirects = new ConcurrentHashMap();
    private final AtomicLong notFoundTotal = new AtomicLong();
    private final AtomicLong redirectTotal = new AtomicLong();
    private volatile Instant since = Instant.now();

    public void recordNotFound(String path) {
        this.notFoundTotal.incrementAndGet();
        if (path == null) {
            return;
        }
        AtomicLong counter = this.bump((Map)this.notFound, path);
        if (counter != null && counter.get() == 1L) {
            log.warn("404 first seen path={}", (Object)path);
        }
    }

    public void recordRedirect(String from, String to) {
        this.redirectTotal.incrementAndGet();
        if (from == null) {
            return;
        }
        AtomicLong counter = this.bump((Map)this.redirects, from);
        if (counter != null && counter.get() == 1L) {
            log.info("301 first seen from={} to={}", (Object)from, (Object)to);
        }
    }

    private AtomicLong bump(Map<String, AtomicLong> map, String key) {
        AtomicLong existing = map.get(key);
        if (existing != null) {
            existing.incrementAndGet();
            return existing;
        }
        if (map.size() >= 500) {
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
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("since", this.since);
        out.put("notFoundTotal", this.notFoundTotal.get());
        out.put("redirectTotal", this.redirectTotal.get());
        out.put("notFound", this.topEntries((Map)this.notFound, limit));
        out.put("redirects", this.topEntries((Map)this.redirects, limit));
        return out;
    }

    private List<Map<String, Object>> topEntries(Map<String, AtomicLong> map, int limit) {
        ArrayList<Map.Entry<String, AtomicLong>> rows = new ArrayList<Map.Entry<String, AtomicLong>>(map.entrySet());
        rows.sort(Comparator.comparingLong(e -> ((AtomicLong)e.getValue()).get()).reversed());
        ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map.Entry entry : rows) {
            if (out.size() >= limit) break;
            LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("path", entry.getKey());
            row.put("count", ((AtomicLong)entry.getValue()).get());
            out.add(row);
        }
        return out;
    }
}

