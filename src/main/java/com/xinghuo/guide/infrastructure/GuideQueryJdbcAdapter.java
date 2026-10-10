package com.xinghuo.guide.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.guide.application.GuideQueryModels;
import com.xinghuo.guide.application.GuideQueryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Guide-owned JDBC projection adapter for public and admin reads. */
@Component
public class GuideQueryJdbcAdapter implements GuideQueryPort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public GuideQueryJdbcAdapter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideQueryModels.GuideSummary> publicList(String gameCode, String query, int limit) {
        StringBuilder sql = new StringBuilder(summarySql());
        sql.append(" WHERE g.status = 'PUBLISHED' AND g.published_at IS NOT NULL AND g.published_at <= NOW()");
        List<Object> args = new ArrayList<>();
        if (gameCode != null && !gameCode.isBlank()) {
            sql.append(" AND g.game_code = ?");
            args.add(gameCode.trim().toUpperCase(Locale.ROOT));
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (g.title LIKE ? OR g.summary LIKE ? OR gc.name LIKE ? OR gc.name_en LIKE ?)");
            String like = "%" + query.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY g.is_featured DESC, g.sort_order ASC, g.published_at DESC, g.id DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 200)));
        return jdbc.queryForList(sql.toString(), args.toArray()).stream().map(this::summary).toList();
    }

    @Override
    @Transactional
    public GuideQueryModels.GuideDetail publicDetail(String slug) {
        Map<String, Object> row = one(summarySql()
                + " WHERE g.slug = ? AND g.status = 'PUBLISHED' AND g.published_at IS NOT NULL AND g.published_at <= NOW()",
                slug);
        if (row == null) {
            throw new NotFoundException();
        }
        Long id = longObject(row.get("id"));
        jdbc.update("UPDATE game_guides SET view_count = view_count + 1 WHERE id = ?", id);
        Map<String, Object> refreshed = one(summarySql() + " WHERE g.id = ?", id);
        return detail(refreshed == null ? row : refreshed, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideQueryModels.GuideSummary> adminList() {
        return jdbc.queryForList(summarySql() + " ORDER BY g.updated_at DESC, g.id DESC")
                .stream()
                .map(this::summary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuideQueryModels.GuideDetail adminDetail(Long id) {
        Map<String, Object> row = one(summarySql() + " WHERE g.id = ?", id);
        if (row == null) {
            throw new NotFoundException();
        }
        return detail(row, false);
    }

    private GuideQueryModels.GuideDetail detail(Map<String, Object> row, boolean publicOnly) {
        GuideQueryModels.GuideSummary guide = summary(row);
        Long id = guide.id();
        List<GuideQueryModels.GuideSection> sections = jdbc.queryForList("""
                SELECT id, section_key, heading, body_markdown, image_url, image_alt, sort_order
                FROM guide_sections WHERE guide_id = ? ORDER BY sort_order ASC, id ASC
                """, id).stream().map(x -> new GuideQueryModels.GuideSection(
                longObject(x.get("id")),
                string(x.get("section_key")),
                string(x.get("heading")),
                string(x.get("body_markdown")),
                string(x.get("image_url")),
                string(x.get("image_alt")),
                intValue(x.get("sort_order")))).toList();
        String productSql = """
                SELECT p.service_id, p.label, p.description, p.image_url AS overlay_image, p.is_featured, p.sort_order AS product_sort,
                       s.slug, s.name, s.category, s.short_desc, s.price_mode, s.price_fixed, s.price_min, s.price_max,
                       s.duration_text, s.cover_image, s.is_active
                FROM guide_products p JOIN services s ON s.id = p.service_id
                WHERE p.guide_id = ?
                """ + (publicOnly ? " AND s.is_active = 1" : "")
                + " ORDER BY p.is_featured DESC, p.sort_order ASC, p.id ASC";
        List<GuideQueryModels.GuideProduct> products = jdbc.queryForList(productSql, id).stream()
                .map(x -> new GuideQueryModels.GuideProduct(
                        longObject(x.get("service_id")),
                        string(x.get("slug")),
                        string(x.get("name")),
                        string(x.get("category")),
                        string(x.get("short_desc")),
                        string(x.get("price_mode")),
                        decimal(x.get("price_fixed")),
                        decimal(x.get("price_min")),
                        decimal(x.get("price_max")),
                        string(x.get("duration_text")),
                        firstNonBlank(string(x.get("overlay_image")), string(x.get("cover_image"))),
                        string(x.get("label")),
                        string(x.get("description")),
                        boolValue(x.get("is_featured")),
                        intValue(x.get("product_sort"))))
                .toList();
        List<GuideQueryModels.GuideFaq> faqs = jdbc.queryForList("""
                SELECT id, question, answer, sort_order FROM guide_faqs
                WHERE guide_id = ? ORDER BY sort_order ASC, id ASC
                """, id).stream().map(x -> new GuideQueryModels.GuideFaq(
                longObject(x.get("id")),
                string(x.get("question")),
                string(x.get("answer")),
                intValue(x.get("sort_order")))).toList();
        List<GuideQueryModels.GuideRelated> related = jdbc.queryForList("""
                SELECT g2.id, g2.game_code, gc.name AS game_name, g2.slug, g2.title, g2.cover_image
                FROM guide_relations r
                JOIN game_guides g2 ON g2.id = r.related_guide_id
                JOIN game_catalog gc ON gc.code = g2.game_code
                WHERE r.guide_id = ? AND g2.status = 'PUBLISHED'
                ORDER BY r.sort_order ASC, g2.id DESC
                """, id).stream().map(x -> new GuideQueryModels.GuideRelated(
                longObject(x.get("id")),
                string(x.get("game_code")),
                string(x.get("game_name")),
                string(x.get("slug")),
                string(x.get("title")),
                string(x.get("cover_image")))).toList();
        return new GuideQueryModels.GuideDetail(guide, sections, products, faqs, related);
    }

    private String summarySql() {
        return """
                SELECT g.id, g.game_code, gc.name AS game_name, g.slug, g.title, g.subtitle, g.summary, g.cover_image,
                       g.difficulty, g.estimated_minutes, g.language, g.author_name, g.source_name, g.source_url,
                       g.status, g.is_featured, g.sort_order, g.tags_json, g.seo_title, g.seo_description,
                       g.canonical_url, g.view_count, g.published_at, g.updated_at,
                       (SELECT COUNT(*) FROM guide_sections s WHERE s.guide_id = g.id) AS section_count,
                       (SELECT COUNT(*) FROM guide_products p WHERE p.guide_id = g.id) AS product_count
                FROM game_guides g JOIN game_catalog gc ON gc.code = g.game_code
                """;
    }

    private GuideQueryModels.GuideSummary summary(Map<String, Object> row) {
        return new GuideQueryModels.GuideSummary(
                longObject(row.get("id")),
                string(row.get("game_code")),
                string(row.get("game_name")),
                string(row.get("slug")),
                string(row.get("title")),
                string(row.get("subtitle")),
                string(row.get("summary")),
                string(row.get("cover_image")),
                string(row.get("difficulty")),
                intValue(row.get("estimated_minutes")),
                string(row.get("language")),
                string(row.get("author_name")),
                string(row.get("source_name")),
                string(row.get("source_url")),
                string(row.get("status")),
                boolValue(row.get("is_featured")),
                intValue(row.get("sort_order")),
                readTags(string(row.get("tags_json"))),
                string(row.get("seo_title")),
                string(row.get("seo_description")),
                string(row.get("canonical_url")),
                longValue(row.get("view_count")),
                instant(row.get("published_at")),
                instant(row.get("updated_at")),
                intValue(row.get("section_count")),
                intValue(row.get("product_count")));
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Long longObject(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean boolValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value instanceof Number number && number.intValue() != 0;
    }

    private java.math.BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        try {
            return Instant.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
