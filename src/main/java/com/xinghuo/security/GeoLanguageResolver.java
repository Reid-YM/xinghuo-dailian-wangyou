package com.xinghuo.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeoLanguageResolver {
    private static final Logger log = LoggerFactory.getLogger(GeoLanguageResolver.class);
    private static final Set<String> ZH_REGIONS = Set.of("CN", "TW", "HK", "MO", "SG");
    private static final int CACHE_LIMIT = 5000;
    private static final long CACHE_TTL_MS = 43200000L;
    private final Map<String, Entry> cache = new ConcurrentHashMap();

    public String detectLanguage(String ip) {
        String country = this.lookupCountry(ip);
        if (country == null || country.isBlank() || "XX".equalsIgnoreCase(country)) {
            return null;
        }
        return ZH_REGIONS.contains(country.toUpperCase(Locale.ROOT)) ? "zh" : "en";
    }

    private String lookupCountry(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        Entry cached = (Entry)this.cache.get(ip);
        if (cached != null && !cached.expired()) {
            return cached.country();
        }
        String country = this.query(ip);
        if (country != null) {
            if (this.cache.size() > 5000) {
                this.cache.clear();
            }
            this.cache.put(ip, new Entry(country, System.currentTimeMillis()));
        }
        return country;
    }

    private String query(String ip) {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create("http://ip-api.com/json/" + ip + "?fields=countryCode");
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String body = r.readLine();
                if (body == null) {
                    return null;
                }
                int i = body.indexOf("\"countryCode\"");
                if (i < 0) {
                    return null;
                }
                int a = body.indexOf('"', body.indexOf(':', i) + 1);
                int b = body.indexOf('"', a + 1);
                return a < 0 || b < 0 ? null : body.substring(a + 1, b);
            }
        } catch (Exception e) {
            log.debug("geo lookup failed for {}: {}", ip, e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private record Entry(String country, long at) {
        boolean expired() {
            return System.currentTimeMillis() - this.at > 43200000L;
        }
    }
}

