/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.xinghuo.config.I18nDictionary
 *  jakarta.annotation.PostConstruct
 *  org.springframework.core.io.ClassPathResource
 *  org.springframework.stereotype.Component
 */
package com.xinghuo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class I18nDictionary {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RESOURCE = "static/assets/i18n-extra.json";
    private Map<String, String> exact = Collections.emptyMap();
    private List<Map.Entry<String, String>> phrases = Collections.emptyList();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream();){
            Map parsed = (Map)MAPPER.readValue(in, (TypeReference)new /* Unavailable Anonymous Inner Class!! */);
            LinkedHashMap clean = new LinkedHashMap();
            parsed.forEach((zh, en) -> {
                if (zh != null && en != null && !zh.isBlank() && !en.isBlank()) {
                    clean.put(zh, en);
                }
            });
            this.exact = Collections.unmodifiableMap(clean);
            ArrayList ordered = new ArrayList(clean.entrySet());
            ordered.sort(Comparator.comparingInt(e -> ((String)e.getKey()).length()).reversed());
            this.phrases = List.copyOf(ordered);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to load static/assets/i18n-extra.json", ex);
        }
    }

    public boolean containsChinese(String value) {
        if (value == null) {
            return false;
        }
        return value.codePoints().anyMatch(cp -> cp >= 19968 && cp <= 40959);
    }

    public String translate(String value) {
        if (value == null || value.isBlank() || !this.containsChinese(value)) {
            return value;
        }
        String direct = (String)this.exact.get(value);
        if (direct != null) {
            return direct;
        }
        StringBuilder result = new StringBuilder(value.length() + 32);
        int index = 0;
        while (index < value.length()) {
            Map.Entry match = null;
            for (Map.Entry entry : this.phrases) {
                String zh = (String)entry.getKey();
                if (zh.length() < 2 || !value.startsWith(zh, index)) continue;
                match = entry;
                break;
            }
            if (match == null) {
                result.append(value.charAt(index++));
                continue;
            }
            result.append((String)match.getValue());
            index += ((String)match.getKey()).length();
        }
        return result.toString();
    }
}

