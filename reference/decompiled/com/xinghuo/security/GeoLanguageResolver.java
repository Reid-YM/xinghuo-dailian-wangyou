/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.security.GeoLanguageResolver
 *  com.xinghuo.security.GeoLanguageResolver$Entry
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.xinghuo.security;

import com.xinghuo.security.GeoLanguageResolver;
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private String query(String ip) {
        HttpURLConnection conn = null;
        try {
            int i;
            String body;
            BufferedReader r;
            block18: {
                block17: {
                    String string;
                    URI uri = URI.create("http://ip-api.com/json/" + ip + "?fields=countryCode");
                    conn = (HttpURLConnection)uri.toURL().openConnection();
                    conn.setConnectTimeout(2500);
                    conn.setReadTimeout(2500);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() != 200) {
                        String string2 = null;
                        return string2;
                    }
                    r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    try {
                        body = r.readLine();
                        if (body != null) break block17;
                        string = null;
                    }
                    catch (Throwable throwable) {
                        try {
                            try {
                                r.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                            throw throwable;
                        }
                        catch (Exception e) {
                            log.debug("geo lookup failed for {}: {}", (Object)ip, (Object)e.getMessage());
                            String string3 = null;
                            return string3;
                        }
                    }
                    r.close();
                    return string;
                }
                i = body.indexOf("\"countryCode\"");
                if (i >= 0) break block18;
                String string = null;
                r.close();
                return string;
            }
            int a = body.indexOf(34, body.indexOf(58, i) + 1);
            int b = body.indexOf(34, a + 1);
            String string = a < 0 || b < 0 ? null : body.substring(a + 1, b);
            r.close();
            return string;
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}

