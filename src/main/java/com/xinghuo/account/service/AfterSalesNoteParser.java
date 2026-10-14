package com.xinghuo.account.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses system after-sales blocks stored in a customer's order note. */
public final class AfterSalesNoteParser {
    private static final Pattern NOTE_BLOCK_HEADER = Pattern.compile(
            "(?:\\A|(?<=\\n\\n))\\[(?:售后问题 · (交付或进度|取消服务|退款核对|其他问题) · ([^\\]\\n]+)|用户补充 ([^\\]\\n]+))\\]\\n");

    private AfterSalesNoteParser() {}

    public static List<AfterSalesRecord> parse(String note) {
        if (note == null || note.isEmpty()) return List.of();
        List<AfterSalesRecord> records = new ArrayList<>();
        Matcher headers = NOTE_BLOCK_HEADER.matcher(note);
        String category = null;
        Instant submittedAt = null;
        int contentStart = -1;
        while (headers.find()) {
            String timestamp = headers.group(2) == null ? headers.group(3) : headers.group(2);
            Instant parsedAt;
            try {
                parsedAt = Instant.parse(timestamp);
            } catch (RuntimeException invalidTimestamp) {
                continue;
            }
            if (category != null) {
                records.add(new AfterSalesRecord(category, submittedAt,
                        note.substring(contentStart, headers.start()).strip()));
            }
            category = headers.group(1);
            submittedAt = category == null ? null : parsedAt;
            contentStart = category == null ? -1 : headers.end();
        }
        if (category != null) {
            records.add(new AfterSalesRecord(category, submittedAt, note.substring(contentStart).strip()));
        }
        return List.copyOf(records);
    }

    public record AfterSalesRecord(String category, Instant submittedAt, String content) {}
}
