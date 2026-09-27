/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.exception.NotFoundException
 *  com.xinghuo.guide.GuideContentService
 *  com.xinghuo.guide.GuideModels$FaqInput
 *  com.xinghuo.guide.GuideModels$GuideDetail
 *  com.xinghuo.guide.GuideModels$GuideFaq
 *  com.xinghuo.guide.GuideModels$GuideImportRequest
 *  com.xinghuo.guide.GuideModels$GuideImportResult
 *  com.xinghuo.guide.GuideModels$GuideProduct
 *  com.xinghuo.guide.GuideModels$GuideRelated
 *  com.xinghuo.guide.GuideModels$GuideSection
 *  com.xinghuo.guide.GuideModels$GuideSummary
 *  com.xinghuo.guide.GuideModels$GuideUpsert
 *  com.xinghuo.guide.GuideModels$ProductInput
 *  com.xinghuo.guide.GuideModels$SectionInput
 *  com.xinghuo.service.AuditService
 *  jakarta.servlet.http.HttpServletRequest
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.jdbc.support.GeneratedKeyHolder
 *  org.springframework.jdbc.support.KeyHolder
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.guide;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.guide.GuideModels;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuideContentService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Set<String> DIFFICULTIES = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AuditService audit;

    public GuideContentService(JdbcTemplate jdbc, ObjectMapper mapper, AuditService audit) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional(readOnly=true)
    public List<GuideModels.GuideSummary> publicList(String gameCode, String query, int limit) {
        StringBuilder sql = new StringBuilder(this.summarySql());
        sql.append(" WHERE g.status = 'PUBLISHED' AND g.published_at IS NOT NULL AND g.published_at <= NOW()");
        ArrayList<Object> args = new ArrayList<Object>();
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
        return this.jdbc.queryForList(sql.toString(), args.toArray()).stream().map(arg_0 -> this.summary(arg_0)).toList();
    }

    @Transactional
    public GuideModels.GuideDetail publicDetail(String slug) {
        Map row = this.one(this.summarySql() + " WHERE g.slug = ? AND g.status = 'PUBLISHED' AND g.published_at IS NOT NULL AND g.published_at <= NOW()", new Object[]{slug});
        if (row == null) {
            throw new NotFoundException();
        }
        Long id = this.longObject(row.get("id"));
        this.jdbc.update("UPDATE game_guides SET view_count = view_count + 1 WHERE id = ?", new Object[]{id});
        Map refreshed = this.one(this.summarySql() + " WHERE g.id = ?", new Object[]{id});
        return this.detail(refreshed == null ? row : refreshed, true);
    }

    @Transactional(readOnly=true)
    public List<GuideModels.GuideSummary> adminList() {
        return this.jdbc.queryForList(this.summarySql() + " ORDER BY g.updated_at DESC, g.id DESC").stream().map(arg_0 -> this.summary(arg_0)).toList();
    }

    @Transactional(readOnly=true)
    public GuideModels.GuideDetail adminDetail(Long id) {
        Map row = this.one(this.summarySql() + " WHERE g.id = ?", new Object[]{id});
        if (row == null) {
            throw new NotFoundException();
        }
        return this.detail(row, false);
    }

    @Transactional
    public GuideModels.GuideDetail create(GuideModels.GuideUpsert input, String operator, HttpServletRequest request) {
        GuideModels.GuideUpsert normalized = this.validateAndNormalize(input);
        Instant now = Instant.now();
        Timestamp publishedAt = "PUBLISHED".equals(normalized.status()) ? Timestamp.from(now) : null;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO game_guides\n(game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes,\n language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json,\n seo_title, seo_description, canonical_url, published_at, created_at, updated_at)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)\n", 1);
            this.bindGuide(ps, normalized, publishedAt, false);
            return ps;
        }, (KeyHolder)keyHolder);
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        this.replaceChildren(id, normalized);
        GuideModels.GuideDetail after = this.adminDetail(id);
        this.audit.log("GUIDE_CREATE", "GAME_GUIDE", id.toString(), operator, null, (Object)after.guide(), normalized.reason(), request);
        return after;
    }

    @Transactional
    public GuideModels.GuideDetail update(Long id, GuideModels.GuideUpsert input, String operator, HttpServletRequest request) {
        GuideModels.GuideUpsert normalized;
        GuideModels.GuideDetail before = this.adminDetail(id);
        Timestamp publishedAt = "PUBLISHED".equals((normalized = this.validateAndNormalize(input)).status()) ? Timestamp.from(Instant.now()) : null;
        int changed = this.jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("UPDATE game_guides SET game_code=?, slug=?, title=?, subtitle=?, summary=?, cover_image=?,\n difficulty=?, estimated_minutes=?, language=?, author_name=?, source_name=?, source_url=?,\n status=?, is_featured=?, sort_order=?, tags_json=?, seo_title=?, seo_description=?,\n canonical_url=?, published_at=CASE WHEN ?='PUBLISHED' THEN COALESCE(published_at, ?) ELSE NULL END,\n updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?\n");
            int index = this.bindGuide(ps, normalized, publishedAt, true);
            ps.setLong(index, id);
            return ps;
        });
        if (changed == 0) {
            throw new NotFoundException();
        }
        this.replaceChildren(id, normalized);
        GuideModels.GuideDetail after = this.adminDetail(id);
        this.audit.log("GUIDE_UPDATE", "GAME_GUIDE", id.toString(), operator, (Object)before.guide(), (Object)after.guide(), normalized.reason(), request);
        return after;
    }

    @Transactional
    public GuideModels.GuideDetail setStatus(Long id, String status, String reason, String operator, HttpServletRequest request) {
        String normalizedStatus = this.normalizeStatus(status);
        GuideModels.GuideDetail before = this.adminDetail(id);
        int changed = "PUBLISHED".equals(normalizedStatus) ? this.jdbc.update("UPDATE game_guides SET status=?, published_at=COALESCE(published_at, CURRENT_TIMESTAMP), updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", new Object[]{normalizedStatus, id}) : this.jdbc.update("UPDATE game_guides SET status=?, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", new Object[]{normalizedStatus, id});
        if (changed == 0) {
            throw new NotFoundException();
        }
        GuideModels.GuideDetail after = this.adminDetail(id);
        this.audit.log("GUIDE_STATUS", "GAME_GUIDE", id.toString(), operator, (Object)before.guide(), (Object)after.guide(), reason, request);
        return after;
    }

    @Transactional
    public void archive(Long id, String reason, String operator, HttpServletRequest request) {
        GuideModels.GuideDetail before = this.adminDetail(id);
        this.jdbc.update("UPDATE game_guides SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", new Object[]{id});
        this.audit.log("GUIDE_ARCHIVE", "GAME_GUIDE", id.toString(), operator, (Object)before.guide(), null, reason, request);
    }

    @Transactional
    public GuideModels.GuideImportResult importGames(GuideModels.GuideImportRequest request, String operator, HttpServletRequest httpRequest) {
        boolean publish = request == null || request.publish() == null || request.publish() != false;
        boolean refresh = request != null && Boolean.TRUE.equals(request.refresh());
        String reason = request == null || request.reason() == null || request.reason().isBlank() ? "\u6309\u6e38\u620f\u4e0e\u670d\u52a1\u76ee\u5f55\u751f\u6210\u591a\u6e38\u620f\u653b\u7565\u5185\u5bb9" : request.reason().trim();
        List games = this.jdbc.queryForList("SELECT gc.code, gc.name, gc.name_en, gc.description, gc.genres, gc.cover_image,\n       gc.publisher, gc.seo_slug, gc.business_status\nFROM game_catalog gc\nWHERE gc.is_active = 1 OR EXISTS (SELECT 1 FROM services s WHERE s.game_code = gc.code AND s.is_active = 1)\nORDER BY gc.sort_order ASC, gc.code ASC\n");
        int created = 0;
        int refreshed = 0;
        int skipped = 0;
        ArrayList<Long> ids = new ArrayList<Long>();
        for (Map game : games) {
            String gameCode = this.string(game.get("code"));
            List services = this.jdbc.queryForList("SELECT s.id, s.slug, s.name, s.category, s.short_desc, s.price_mode, s.price_fixed, s.price_min, s.price_max,\n       s.duration_text, s.cover_image, s.hot_score, s.sort_order, s.manual_priority\nFROM services s\nJOIN game_catalog gc ON gc.code = s.game_code\nJOIN service_categories sc ON sc.game_code = s.game_code AND sc.code = s.category\nWHERE s.game_code = ? AND s.is_active = 1 AND gc.is_active = 1 AND sc.is_active = 1\n  AND s.status <> 'OFFLINE'\n  AND (s.published_at IS NULL OR s.published_at <= NOW())\n  AND (s.expires_at IS NULL OR s.expires_at > NOW())\nORDER BY (s.hot_score * 10 + s.manual_priority) DESC, s.sort_order ASC, s.id ASC\n", new Object[]{gameCode});
            if (services.isEmpty()) {
                ++skipped;
                continue;
            }
            Map existing = this.one("SELECT id, slug FROM game_guides WHERE game_code = ? ORDER BY id ASC LIMIT 1", new Object[]{gameCode});
            if (existing != null && !refresh) {
                ++skipped;
                continue;
            }
            String slug = existing == null ? this.uniqueGuideSlug(this.slugify(gameCode) + "-guide") : this.string(existing.get("slug"));
            GuideModels.GuideUpsert generated = this.generatedGuide(game, services, slug, publish, reason);
            if (existing == null) {
                GuideModels.GuideDetail detail = this.create(generated, operator, httpRequest);
                ids.add(detail.guide().id());
                ++created;
                continue;
            }
            Long id = this.longObject(existing.get("id"));
            GuideModels.GuideDetail detail = this.update(id, generated, operator, httpRequest);
            ids.add(detail.guide().id());
            ++refreshed;
        }
        return new GuideModels.GuideImportResult(games.size(), created, refreshed, skipped, ids);
    }

    private GuideModels.GuideUpsert generatedGuide(Map<String, Object> game, List<Map<String, Object>> services, String slug, boolean publish, String reason) {
        String gameCode = this.string(game.get("code"));
        String gameName = this.string(game.get("name"));
        String description = this.string(game.get("description"));
        String publisher = this.string(game.get("publisher"));
        List<String> serviceNames = services.stream().map(x -> this.string(x.get("name"))).filter(x -> !x.isBlank()).toList();
        List tags = this.splitTags(this.string(game.get("genres")));
        tags.addAll(serviceNames.stream().limit(4L).toList());
        List<String> uniqueTags = tags.stream().filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().limit(8L).toList();
        String summary = description == null || description.isBlank() ? gameName + " \u5f53\u524d\u53ef\u63a5\u670d\u52a1\u4e0e\u4efb\u52a1\u8def\u5f84\u6c47\u603b\u3002\u6309\u8d26\u53f7\u5f53\u524d\u8fdb\u5ea6\u4e0e\u76ee\u6807\u62a5\u4ef7\uff0c\u7f51\u9875\u4e0d\u6536\u53d6\u8d26\u53f7\u5bc6\u7801\u3001\u9a8c\u8bc1\u7801\u6216\u4ee4\u724c\u3002" : description.trim() + " \u672c\u653b\u7565\u6c47\u603b\u5f53\u524d\u53ef\u63a5\u670d\u52a1\u3001\u63a8\u8fdb\u987a\u5e8f\u4e0e\u5b89\u5168\u4ea4\u4ed8\u6ce8\u610f\u4e8b\u9879\u3002";
        List<GuideModels.SectionInput> sections = List.of(new GuideModels.SectionInput("service-map", gameName + " \u5f53\u524d\u53ef\u63a5\u670d\u52a1", this.serviceMapBody(gameName, services), null, "1", Integer.valueOf(10)), new GuideModels.SectionInput("route", "\u63a8\u8350\u63a8\u8fdb\u987a\u5e8f", this.routeBody(gameName, services), null, null, Integer.valueOf(20)), new GuideModels.SectionInput("safety", "\u4ea4\u4ed8\u4e0e\u8d26\u53f7\u5b89\u5168", "\u7f51\u9875\u53ea\u6536\u96c6\u9700\u6c42\u3001\u622a\u56fe\u4e0e\u8054\u7cfb\u65b9\u5f0f\uff0c\u4e0d\u6536\u8d26\u53f7\u5bc6\u7801\u3001\u9a8c\u8bc1\u7801\u6216\u4ee4\u724c\u3002\n\n\u786e\u8ba4\u62a5\u4ef7\u540e\u8d70\u6dd8\u5b9d\u6216\u95f2\u9c7c\u62c5\u4fdd\u4ea4\u6613\u3002\u5f00\u5de5\u524d\u8bf7\u6838\u5bf9\u533a\u670d\u3001\u5e73\u53f0\u3001\u76ee\u6807\u3001\u53ef\u64cd\u4f5c\u65f6\u95f4\u548c\u5f02\u5e38\u5904\u7406\u65b9\u5f0f\uff1b\u786e\u8ba4\u4e0d\u6ee1\u8db3\u6761\u4ef6\u65f6\uff0c\u4e0d\u627f\u8bfa\u7acb\u5373\u5c65\u7ea6\u3002\n\n\u5982\u679c\u9700\u8981\u5206\u9636\u6bb5\u63a8\u8fdb\uff0c\u53ef\u4ee5\u5148\u628a\u5f53\u524d\u622a\u56fe\u6216\u8fdb\u5ea6\u8bf4\u660e\u53d1\u5ba2\u670d\uff0c\u7531\u5ba2\u670d\u6309\u9636\u6bb5\u62c6\u5206\u4efb\u52a1\uff0c\u907f\u514d\u4e00\u6b21\u627f\u8bfa\u8fc7\u591a\u5185\u5bb9\u3002", null, null, Integer.valueOf(30)), new GuideModels.SectionInput("checklist", "\u4e0b\u5355\u524d\u68c0\u67e5\u6e05\u5355", "1. \u786e\u8ba4\u6e38\u620f\u533a\u670d\u4e0e\u767b\u5f55\u5e73\u53f0\uff0c\u4f8b\u5982 Steam\u3001Battle.net\u3001PlayStation\u3001Xbox\u3001\u79fb\u52a8\u7aef\u6216\u56fd\u670d\u3002\n2. \u786e\u8ba4\u5f53\u524d\u8fdb\u5ea6\u548c\u76ee\u6807\u8fdb\u5ea6\uff0c\u6700\u597d\u51c6\u5907\u89d2\u8272\u3001\u6bb5\u4f4d\u3001\u4efb\u52a1\u6216\u5e93\u5b58\u622a\u56fe\u3002\n3. \u8bf4\u660e\u6bcf\u5929\u53ef\u64cd\u4f5c\u65f6\u6bb5\u548c\u662f\u5426\u5141\u8bb8\u7ec4\u961f\uff0c\u51cf\u5c11\u7b49\u5f85\u4e0e\u51b2\u7a81\u3002\n4. \u5982\u6d89\u53ca\u6d3b\u52a8\u622a\u6b62\u3001\u8d5b\u5b63\u7ed3\u7b97\u6216\u5468\u8d5b\u8d44\u683c\uff0c\u5148\u786e\u8ba4\u5269\u4f59\u65f6\u95f4\u662f\u5426\u8db3\u591f\u3002\n5. \u4ef7\u683c\u4ee5\u4eba\u5de5\u786e\u8ba4\u540e\u7684\u62c5\u4fdd\u8ba2\u5355\u4e3a\u51c6\uff0c\u786e\u8ba4\u524d\u4e0d\u8981\u652f\u4ed8\u3002", null, null, Integer.valueOf(40)));
        ArrayList<GuideModels.ProductInput> products = new ArrayList<GuideModels.ProductInput>();
        int order = 10;
        for (Map<String, Object> service : services) {
            products.add(new GuideModels.ProductInput(this.longObject(service.get("id")), this.string(service.get("name")), this.string(service.get("short_desc")), null, Boolean.valueOf(order <= 20), Integer.valueOf(order)));
            order += 10;
        }
        List<GuideModels.FaqInput> faqs = List.of(new GuideModels.FaqInput("\u73b0\u5728\u53ef\u4ee5\u76f4\u63a5\u4e0b\u5355\u5417\uff1f", "\u9875\u9762\u4f1a\u663e\u793a\u5f53\u524d\u8fd0\u8425\u72b6\u6001\u3002\u5904\u4e8e\u9884\u7ea6\u6216\u53ef\u9884\u7ea6\u72b6\u6001\u65f6\uff0c\u53ef\u4ee5\u5148\u63d0\u4ea4\u9700\u6c42\uff1b\u5ba2\u670d\u786e\u8ba4\u62a5\u4ef7\u3001\u533a\u670d\u4e0e\u65f6\u95f4\u540e\u624d\u5b89\u6392\u5f00\u5de5\u3002", Integer.valueOf(10)), new GuideModels.FaqInput("\u9700\u8981\u63d0\u4f9b\u8d26\u53f7\u5bc6\u7801\u5417\uff1f", "\u4e0d\u9700\u8981\u3002\u653b\u7565\u9875\u548c\u8be2\u4ef7\u9875\u90fd\u4e0d\u8981\u6c42\u8d26\u53f7\u5bc6\u7801\u3001\u9a8c\u8bc1\u7801\u6216\u767b\u5f55\u4ee4\u724c\uff1b\u5177\u4f53\u4e0a\u53f7\u6216\u7ec4\u961f\u65b9\u5f0f\u53ea\u5728\u5ba2\u670d\u786e\u8ba4\u540e\u5355\u72ec\u6c9f\u901a\u3002", Integer.valueOf(20)), new GuideModels.FaqInput("\u4ef7\u683c\u600e\u4e48\u8ba1\u7b97\uff1f", "\u6309\u5f53\u524d\u8fdb\u5ea6\u5230\u76ee\u6807\u8fdb\u5ea6\u7684\u5dee\u8ddd\u3001\u9884\u8ba1\u8017\u65f6\u548c\u6d3b\u52a8\u622a\u6b62\u65f6\u95f4\u7efc\u5408\u62a5\u4ef7\u3002\u7f51\u9875\u5c55\u793a\u7684\u53c2\u8003\u4ef7\u4e0d\u662f\u6700\u7ec8\u7ed3\u7b97\u4ef7\uff0c\u6700\u7ec8\u4ee5\u62c5\u4fdd\u8ba2\u5355\u786e\u8ba4\u91d1\u989d\u4e3a\u51c6\u3002", Integer.valueOf(30)));
        String difficulty = services.stream().anyMatch(x -> Set.of("RANKED", "CHAMPION", "EVENT", "CAMO", "POWER").contains(this.string(x.get("category")).toUpperCase(Locale.ROOT))) ? "INTERMEDIATE" : "BEGINNER";
        String status = publish ? "PUBLISHED" : "DRAFT";
        String cover = this.string(game.get("cover_image"));
        String canonical = "https://wydlgf.cn/guides/" + slug;
        return new GuideModels.GuideUpsert(gameCode, slug, gameName + " \u670d\u52a1\u653b\u7565\u4e0e\u4efb\u52a1\u8def\u5f84", gameName + " \u591a\u670d\u52a1\u5bf9\u6bd4 \u00b7 \u63a8\u8fdb\u987a\u5e8f \u00b7 \u5b89\u5168\u4ea4\u4ed8", summary, cover, difficulty, Integer.valueOf(60), "zh-CN", "\u661f\u706b\u653b\u7565\u7ec4", publisher, null, status, Boolean.valueOf(true), Integer.valueOf(100), uniqueTags, gameName + " \u4ee3\u7ec3\u670d\u52a1\u653b\u7565 | \u661f\u706b\u7f51\u6e38\u4ee3\u7ec3\u5e73\u53f0", summary, canonical, sections, products, faqs, List.of(), reason);
    }

    private String serviceMapBody(String gameName, List<Map<String, Object>> services) {
        StringBuilder out = new StringBuilder();
        out.append("\u622a\u81f3\u5f53\u524d\uff0c").append(gameName).append("\u53ef\u63a5\u670d\u52a1\u5305\u62ec\uff1a\n\n");
        for (Map<String, Object> service : services) {
            out.append("- **").append(this.string(service.get("name"))).append("**\uff1a").append(this.string(service.get("short_desc"))).append("\uff08").append(this.string(service.get("duration_text"))).append("\uff09\n");
        }
        out.append("\n\u4e0d\u540c\u670d\u52a1\u7684\u8ba1\u4ef7\u65b9\u5f0f\u4e0d\u540c\uff1a\u56fa\u5b9a\u4ef7\u9002\u5408\u660e\u786e\u76ee\u6807\uff0c\u533a\u95f4\u4ef7\u9002\u5408\u6309\u5f53\u524d\u8fdb\u5ea6\u4f30\u7b97\uff0c\u6700\u7ec8\u4ee5\u5ba2\u670d\u786e\u8ba4\u7684\u4e1a\u52a1\u5355\u4e3a\u51c6\u3002");
        return out.toString();
    }

    private String routeBody(String gameName, List<Map<String, Object>> services) {
        String first = services.isEmpty() ? "\u5f53\u524d\u670d\u52a1" : this.string(services.get(0).get("name"));
        String second = services.size() < 2 ? "\u540e\u7eed\u76ee\u6807" : this.string(services.get(1).get("name"));
        return "\u63a8\u8350\u5148\u5904\u7406\u4f1a\u5f71\u54cd\u540e\u7eed\u6548\u7387\u7684\u57fa\u7840\u8fdb\u5ea6\uff0c\u518d\u8fdb\u5165\u9ad8\u4ef7\u503c\u76ee\u6807\u3002\n\n1. \u5148\u786e\u8ba4 " + gameName + " \u5f53\u524d\u8d26\u53f7\u72b6\u6001\u3001\u53ef\u7528\u5e73\u53f0\u4e0e\u76ee\u6807\u662f\u5426\u53ef\u6267\u884c\u3002\n2. \u4f18\u5148\u5b89\u6392\u300c" + first + "\u300d\uff0c\u628a\u57fa\u7840\u8fdb\u5ea6\u6216\u901a\u884c\u8bc1\u7c7b\u5185\u5bb9\u8865\u9f50\u3002\n3. \u518d\u8bc4\u4f30\u300c" + second + "\u300d\u7b49\u76ee\u6807\uff0c\u907f\u514d\u56e0\u4e3a\u6d3b\u52a8\u622a\u6b62\u6216\u6bb5\u4f4d\u6ce2\u52a8\u9020\u6210\u91cd\u590d\u62a5\u4ef7\u3002\n4. \u5982\u679c\u76ee\u6807\u662f\u6392\u884c\u699c\u3001\u5468\u8d5b\u8d44\u683c\u6216\u9650\u65f6\u6311\u6218\uff0c\u5148\u63d0\u4ea4\u622a\u6b62\u65f6\u95f4\u548c\u5f53\u524d\u622a\u56fe\uff0c\u7531\u5ba2\u670d\u5224\u65ad\u662f\u5426\u9700\u8981\u52a0\u6025\u3002";
    }

    private GuideModels.GuideUpsert validateAndNormalize(GuideModels.GuideUpsert input) {
        List sections;
        String difficulty;
        if (input == null) {
            throw new BadRequestException("\u653b\u7565\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String gameCode = this.required(input.gameCode(), "\u8bf7\u9009\u62e9\u6e38\u620f").trim().toUpperCase(Locale.ROOT);
        Map game = this.one("SELECT code FROM game_catalog WHERE code = ?", new Object[]{gameCode});
        if (game == null) {
            throw new BadRequestException("\u6e38\u620f\u4e0d\u5b58\u5728");
        }
        String title = this.required(input.title(), "\u8bf7\u586b\u5199\u653b\u7565\u6807\u9898").trim();
        String slug = this.slugify(input.slug() == null || input.slug().isBlank() ? title : input.slug());
        if (slug.isBlank()) {
            slug = this.slugify(gameCode + "-guide");
        }
        String status = this.normalizeStatus(input.status() == null || input.status().isBlank() ? "DRAFT" : input.status());
        String string = difficulty = input.difficulty() == null || input.difficulty().isBlank() ? "BEGINNER" : input.difficulty().trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(difficulty)) {
            throw new BadRequestException("\u975e\u6cd5\u96be\u5ea6\u7b49\u7ea7");
        }
        List<Object> list = sections = input.sections() == null ? List.of() : input.sections().stream().filter(Objects::nonNull).filter(x -> x.heading() != null && !x.heading().isBlank()).map(x -> new GuideModels.SectionInput(x.sectionKey() == null || x.sectionKey().isBlank() ? this.slugify(x.heading()) : this.slugify(x.sectionKey()), x.heading().trim(), x.bodyMarkdown() == null ? "" : x.bodyMarkdown().trim(), this.trimNull(x.imageUrl()), this.trimNull(x.imageAlt()), Integer.valueOf(x.sortOrder() == null ? 10 : x.sortOrder()))).toList();
        if ("PUBLISHED".equals(status) && sections.isEmpty()) {
            throw new BadRequestException("\u53d1\u5e03\u653b\u7565\u524d\u81f3\u5c11\u9700\u8981\u4e00\u4e2a\u5185\u5bb9\u6bb5\u843d");
        }
        List<GuideModels.ProductInput> products = input.products() == null ? List.of() : input.products().stream().filter(Objects::nonNull).filter(x -> x.serviceId() != null).map(x -> new GuideModels.ProductInput(x.serviceId(), this.trimNull(x.label()), this.trimNull(x.description()), this.trimNull(x.imageUrl()), Boolean.valueOf(x.featured() != null && x.featured() != false), Integer.valueOf(x.sortOrder() == null ? 10 : x.sortOrder()))).toList();
        for (GuideModels.ProductInput product : products) {
            Map service = this.one("SELECT game_code FROM services WHERE id = ?", new Object[]{product.serviceId()});
            if (service != null && gameCode.equals(this.string(service.get("game_code")))) continue;
            throw new BadRequestException("\u63a8\u8350\u5546\u54c1\u4e0d\u5c5e\u4e8e\u5f53\u524d\u6e38\u620f");
        }
        List faqs = input.faqs() == null ? List.of() : input.faqs().stream().filter(Objects::nonNull).filter(x -> x.question() != null && !x.question().isBlank() && x.answer() != null && !x.answer().isBlank()).map(x -> new GuideModels.FaqInput(x.question().trim(), x.answer().trim(), Integer.valueOf(x.sortOrder() == null ? 10 : x.sortOrder()))).toList();
        List tags = input.tags() == null ? List.of() : input.tags().stream().filter(Objects::nonNull).map(String::trim).filter(x -> !x.isBlank()).distinct().limit(12L).toList();
        List relatedIds = input.relatedGuideIds() == null ? List.of() : input.relatedGuideIds().stream().filter(Objects::nonNull).distinct().limit(12L).toList();
        return new GuideModels.GuideUpsert(gameCode, slug, title, this.trimNull(input.subtitle()), this.required(input.summary(), "\u8bf7\u586b\u5199\u653b\u7565\u6458\u8981").trim(), this.trimNull(input.coverImage()), difficulty, Integer.valueOf(input.estimatedMinutes() == null ? 30 : Math.max(1, Math.min(input.estimatedMinutes(), 10080))), input.language() == null || input.language().isBlank() ? "zh-CN" : input.language().trim(), input.authorName() == null || input.authorName().isBlank() ? "\u661f\u706b\u653b\u7565\u7ec4" : input.authorName().trim(), this.trimNull(input.sourceName()), this.trimNull(input.sourceUrl()), status, Boolean.valueOf(input.featured() != null && input.featured() != false), Integer.valueOf(input.sortOrder() == null ? 100 : Math.max(0, input.sortOrder())), tags, this.trimNull(input.seoTitle()), this.trimNull(input.seoDescription()), this.trimNull(input.canonicalUrl()), sections, products, faqs, relatedIds, this.trimNull(input.reason()));
    }

    private int bindGuide(PreparedStatement ps, GuideModels.GuideUpsert guide, Timestamp publishedAt, boolean update) throws SQLException {
        int i = 1;
        ps.setString(i++, guide.gameCode());
        ps.setString(i++, guide.slug());
        ps.setString(i++, guide.title());
        ps.setString(i++, guide.subtitle());
        ps.setString(i++, guide.summary());
        ps.setString(i++, guide.coverImage());
        ps.setString(i++, guide.difficulty());
        ps.setInt(i++, guide.estimatedMinutes());
        ps.setString(i++, guide.language());
        ps.setString(i++, guide.authorName());
        ps.setString(i++, guide.sourceName());
        ps.setString(i++, guide.sourceUrl());
        ps.setString(i++, guide.status());
        ps.setBoolean(i++, guide.featured());
        ps.setInt(i++, guide.sortOrder());
        ps.setString(i++, this.writeTags(guide.tags()));
        ps.setString(i++, guide.seoTitle());
        ps.setString(i++, guide.seoDescription());
        ps.setString(i++, guide.canonicalUrl());
        if (update) {
            ps.setString(i++, guide.status());
            ps.setTimestamp(i++, publishedAt);
        } else if (publishedAt == null) {
            ps.setNull(i++, 93);
        } else {
            ps.setTimestamp(i++, publishedAt);
        }
        return i;
    }

    private void replaceChildren(Long guideId, GuideModels.GuideUpsert guide) {
        this.jdbc.update("DELETE FROM guide_sections WHERE guide_id = ?", new Object[]{guideId});
        int sectionOrder = 10;
        for (Object section : guide.sections()) {
            this.jdbc.update("INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order, created_at, updated_at)\nVALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)\n", new Object[]{guideId, section.sectionKey(), section.heading(), section.bodyMarkdown(), section.imageUrl(), section.imageAlt(), section.sortOrder() == 0 ? sectionOrder : section.sortOrder()});
            sectionOrder += 10;
        }
        this.jdbc.update("DELETE FROM guide_products WHERE guide_id = ?", new Object[]{guideId});
        int productOrder = 10;
        for (GuideModels.ProductInput product : guide.products()) {
            this.jdbc.update("INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order, created_at, updated_at)\nVALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)\n", new Object[]{guideId, product.serviceId(), product.label(), product.description(), product.imageUrl(), product.featured(), product.sortOrder() == 0 ? productOrder : product.sortOrder()});
            productOrder += 10;
        }
        this.jdbc.update("DELETE FROM guide_faqs WHERE guide_id = ?", new Object[]{guideId});
        int faqOrder = 10;
        for (GuideModels.FaqInput faq : guide.faqs()) {
            this.jdbc.update("INSERT INTO guide_faqs (guide_id, question, answer, sort_order, created_at, updated_at)\nVALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)\n", new Object[]{guideId, faq.question(), faq.answer(), faq.sortOrder() == 0 ? faqOrder : faq.sortOrder()});
            faqOrder += 10;
        }
        this.jdbc.update("DELETE FROM guide_relations WHERE guide_id = ?", new Object[]{guideId});
        for (Long relatedId : guide.relatedGuideIds()) {
            if (relatedId == null || relatedId.equals(guideId) || (Integer)this.jdbc.queryForObject("SELECT COUNT(*) FROM game_guides WHERE id = ?", Integer.class, new Object[]{relatedId}) <= 0) continue;
            this.jdbc.update("INSERT INTO guide_relations (guide_id, related_guide_id, sort_order, created_at) VALUES (?, ?, 10, CURRENT_TIMESTAMP)", new Object[]{guideId, relatedId});
        }
    }

    private GuideModels.GuideDetail detail(Map<String, Object> row, boolean publicOnly) {
        GuideModels.GuideSummary guide = this.summary(row);
        Long id = guide.id();
        List<GuideModels.GuideSection> sections = this.jdbc.queryForList("SELECT id, section_key, heading, body_markdown, image_url, image_alt, sort_order\nFROM guide_sections WHERE guide_id = ? ORDER BY sort_order ASC, id ASC\n", new Object[]{id}).stream().map(x -> new GuideModels.GuideSection(this.longObject(x.get("id")), this.string(x.get("section_key")), this.string(x.get("heading")), this.string(x.get("body_markdown")), this.string(x.get("image_url")), this.string(x.get("image_alt")), this.intValue(x.get("sort_order")))).toList();
        String productSql = "SELECT p.service_id, p.label, p.description, p.image_url AS overlay_image, p.is_featured, p.sort_order AS product_sort,\n       s.slug, s.name, s.category, s.short_desc, s.price_mode, s.price_fixed, s.price_min, s.price_max,\n       s.duration_text, s.cover_image, s.is_active\nFROM guide_products p JOIN services s ON s.id = p.service_id\nWHERE p.guide_id = ?\n" + (publicOnly ? " AND s.is_active = 1" : "") + " ORDER BY p.is_featured DESC, p.sort_order ASC, p.id ASC";
        List<GuideModels.GuideProduct> products = this.jdbc.queryForList(productSql, new Object[]{id}).stream().map(x -> new GuideModels.GuideProduct(this.longObject(x.get("service_id")), this.string(x.get("slug")), this.string(x.get("name")), this.string(x.get("category")), this.string(x.get("short_desc")), this.string(x.get("price_mode")), this.decimal(x.get("price_fixed")), this.decimal(x.get("price_min")), this.decimal(x.get("price_max")), this.string(x.get("duration_text")), this.firstNonBlank(this.string(x.get("overlay_image")), this.string(x.get("cover_image"))), this.string(x.get("label")), this.string(x.get("description")), this.boolValue(x.get("is_featured")), this.intValue(x.get("product_sort")))).toList();
        List<GuideModels.GuideFaq> faqs = this.jdbc.queryForList("SELECT id, question, answer, sort_order FROM guide_faqs WHERE guide_id = ? ORDER BY sort_order ASC, id ASC\n", new Object[]{id}).stream().map(x -> new GuideModels.GuideFaq(this.longObject(x.get("id")), this.string(x.get("question")), this.string(x.get("answer")), this.intValue(x.get("sort_order")))).toList();
        List<GuideModels.GuideRelated> related = this.jdbc.queryForList("SELECT g2.id, g2.game_code, gc.name AS game_name, g2.slug, g2.title, g2.cover_image\nFROM guide_relations r\nJOIN game_guides g2 ON g2.id = r.related_guide_id\nJOIN game_catalog gc ON gc.code = g2.game_code\nWHERE r.guide_id = ? AND g2.status = 'PUBLISHED'\nORDER BY r.sort_order ASC, g2.id DESC\n", new Object[]{id}).stream().map(x -> new GuideModels.GuideRelated(this.longObject(x.get("id")), this.string(x.get("game_code")), this.string(x.get("game_name")), this.string(x.get("slug")), this.string(x.get("title")), this.string(x.get("cover_image")))).toList();
        return new GuideModels.GuideDetail(guide, sections, products, faqs, related);
    }

    private String summarySql() {
        return "SELECT g.id, g.game_code, gc.name AS game_name, g.slug, g.title, g.subtitle, g.summary, g.cover_image,\n       g.difficulty, g.estimated_minutes, g.language, g.author_name, g.source_name, g.source_url,\n       g.status, g.is_featured, g.sort_order, g.tags_json, g.seo_title, g.seo_description,\n       g.canonical_url, g.view_count, g.published_at, g.updated_at,\n       (SELECT COUNT(*) FROM guide_sections s WHERE s.guide_id = g.id) AS section_count,\n       (SELECT COUNT(*) FROM guide_products p WHERE p.guide_id = g.id) AS product_count\nFROM game_guides g JOIN game_catalog gc ON gc.code = g.game_code\n";
    }

    private GuideModels.GuideSummary summary(Map<String, Object> row) {
        return new GuideModels.GuideSummary(this.longObject(row.get("id")), this.string(row.get("game_code")), this.string(row.get("game_name")), this.string(row.get("slug")), this.string(row.get("title")), this.string(row.get("subtitle")), this.string(row.get("summary")), this.string(row.get("cover_image")), this.string(row.get("difficulty")), this.intValue(row.get("estimated_minutes")), this.string(row.get("language")), this.string(row.get("author_name")), this.string(row.get("source_name")), this.string(row.get("source_url")), this.string(row.get("status")), this.boolValue(row.get("is_featured")), this.intValue(row.get("sort_order")), this.readTags(this.string(row.get("tags_json"))), this.string(row.get("seo_title")), this.string(row.get("seo_description")), this.string(row.get("canonical_url")), this.longValue(row.get("view_count")), this.instant(row.get("published_at")), this.instant(row.get("updated_at")), this.intValue(row.get("section_count")), this.intValue(row.get("product_count")));
    }

    private Map<String, Object> one(String sql, Object ... args) {
        List rows = this.jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : (Map)rows.get(0);
    }

    private String uniqueGuideSlug(String base) {
        Object candidate = base;
        int suffix = 2;
        while ((Integer)this.jdbc.queryForObject("SELECT COUNT(*) FROM game_guides WHERE slug = ?", Integer.class, new Object[]{candidate}) > 0) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String out = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return out.length() > 120 ? out.substring(0, 120).replaceAll("-+$", "") : out;
    }

    private String normalizeStatus(String status) {
        String normalized;
        String string = normalized = status == null ? "DRAFT" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new BadRequestException("\u975e\u6cd5\u653b\u7565\u72b6\u6001");
        }
        return normalized;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private String trimNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writeTags(List<String> tags) {
        try {
            return this.mapper.writeValueAsString(tags == null ? List.of() : tags);
        }
        catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return (List)this.mapper.readValue(json, (TypeReference)new /* Unavailable Anonymous Inner Class!! */);
        }
        catch (Exception e) {
            return List.of();
        }
    }

    private List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(Arrays.stream(raw.split("[,\uff0c/|\u3001]+")).map(String::trim).filter(x -> !x.isBlank()).toList());
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long longValue(Object value) {
        long l;
        if (value instanceof Number) {
            Number number = (Number)value;
            l = number.longValue();
        } else {
            l = 0L;
        }
        return l;
    }

    private Long longObject(Object value) {
        Long l;
        if (value instanceof Number) {
            Number number = (Number)value;
            l = number.longValue();
        } else {
            l = null;
        }
        return l;
    }

    private int intValue(Object value) {
        int n;
        if (value instanceof Number) {
            Number number = (Number)value;
            n = number.intValue();
        } else {
            n = 0;
        }
        return n;
    }

    private boolean boolValue(Object value) {
        Number number;
        if (value instanceof Boolean) {
            Boolean bool = (Boolean)value;
            return bool;
        }
        return value instanceof Number && (number = (Number)value).intValue() != 0;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        }
        catch (Exception e) {
            return null;
        }
    }

    private Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            Timestamp timestamp = (Timestamp)value;
            return timestamp.toInstant();
        }
        try {
            return Instant.parse(String.valueOf(value));
        }
        catch (Exception e) {
            return null;
        }
    }
}

