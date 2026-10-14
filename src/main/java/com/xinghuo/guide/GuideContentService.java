package com.xinghuo.guide;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.PreparedStatement;
import java.sql.Statement;
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

    @Transactional(readOnly = true)
    public List<GuideModels.GuideSummary> publicList(String gameCode, String query, int limit) {
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

    @Transactional
    public GuideModels.GuideDetail publicDetail(String slug) {
        Map<String, Object> row = one(summarySql() + " WHERE g.slug = ? AND g.status = 'PUBLISHED' AND g.published_at IS NOT NULL AND g.published_at <= NOW()", slug);
        if (row == null) {
            throw new NotFoundException();
        }
        Long id = longObject(row.get("id"));
        jdbc.update("UPDATE game_guides SET view_count = view_count + 1 WHERE id = ?", id);
        Map<String, Object> refreshed = one(summarySql() + " WHERE g.id = ?", id);
        return detail(refreshed == null ? row : refreshed, true);
    }

    @Transactional(readOnly = true)
    public List<GuideModels.GuideSummary> adminList() {
        return jdbc.queryForList(summarySql() + " ORDER BY g.updated_at DESC, g.id DESC").stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public GuideModels.GuideDetail adminDetail(Long id) {
        Map<String, Object> row = one(summarySql() + " WHERE g.id = ?", id);
        if (row == null) {
            throw new NotFoundException();
        }
        return detail(row, false);
    }

    @Transactional
    public GuideModels.GuideDetail create(GuideModels.GuideUpsert input, String operator, HttpServletRequest request) {
        GuideModels.GuideUpsert normalized = validateAndNormalize(input);
        Instant now = Instant.now();
        Timestamp publishedAt = "PUBLISHED".equals(normalized.status()) ? Timestamp.from(now) : null;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO game_guides
                    (game_code, slug, title, subtitle, summary, cover_image, difficulty, estimated_minutes,
                     language, author_name, source_name, source_url, status, is_featured, sort_order, tags_json,
                     seo_title, seo_description, canonical_url, published_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            bindGuide(ps, normalized, publishedAt, false);
            return ps;
        }, keyHolder);
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        replaceChildren(id, normalized);
        GuideModels.GuideDetail after = adminDetail(id);
        audit.log("GUIDE_CREATE", "GAME_GUIDE", id.toString(), operator, null, after.guide(), normalized.reason(), request);
        return after;
    }

    @Transactional
    public GuideModels.GuideDetail update(Long id, GuideModels.GuideUpsert input, String operator, HttpServletRequest request) {
        GuideModels.GuideDetail before = adminDetail(id);
        GuideModels.GuideUpsert normalized = validateAndNormalize(input);
        Timestamp publishedAt = "PUBLISHED".equals(normalized.status()) ? Timestamp.from(Instant.now()) : null;
        int changed = jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    UPDATE game_guides SET game_code=?, slug=?, title=?, subtitle=?, summary=?, cover_image=?,
                     difficulty=?, estimated_minutes=?, language=?, author_name=?, source_name=?, source_url=?,
                     status=?, is_featured=?, sort_order=?, tags_json=?, seo_title=?, seo_description=?,
                     canonical_url=?, published_at=CASE WHEN ?='PUBLISHED' THEN COALESCE(published_at, ?) ELSE NULL END,
                     updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?
                    """);
            int index = bindGuide(ps, normalized, publishedAt, true);
            ps.setLong(index, id);
            return ps;
        });
        if (changed == 0) {
            throw new NotFoundException();
        }
        replaceChildren(id, normalized);
        GuideModels.GuideDetail after = adminDetail(id);
        audit.log("GUIDE_UPDATE", "GAME_GUIDE", id.toString(), operator, before.guide(), after.guide(), normalized.reason(), request);
        return after;
    }

    @Transactional
    public GuideModels.GuideDetail setStatus(Long id, String status, String reason, String operator, HttpServletRequest request) {
        String normalizedStatus = normalizeStatus(status);
        GuideModels.GuideDetail before = adminDetail(id);
        int changed;
        if ("PUBLISHED".equals(normalizedStatus)) {
            changed = jdbc.update("UPDATE game_guides SET status=?, published_at=COALESCE(published_at, CURRENT_TIMESTAMP), updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", normalizedStatus, id);
        } else {
            changed = jdbc.update("UPDATE game_guides SET status=?, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", normalizedStatus, id);
        }
        if (changed == 0) {
            throw new NotFoundException();
        }
        GuideModels.GuideDetail after = adminDetail(id);
        audit.log("GUIDE_STATUS", "GAME_GUIDE", id.toString(), operator, before.guide(), after.guide(), reason, request);
        return after;
    }

    @Transactional
    public void archive(Long id, String reason, String operator, HttpServletRequest request) {
        GuideModels.GuideDetail before = adminDetail(id);
        jdbc.update("UPDATE game_guides SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", id);
        audit.log("GUIDE_ARCHIVE", "GAME_GUIDE", id.toString(), operator, before.guide(), null, reason, request);
    }

    @Transactional
    public GuideModels.GuideBatchArchiveResult archiveBatch(List<Long> ids, String reason, String operator, HttpServletRequest request) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("请选择要归档的攻略");
        }
        List<Long> uniqueIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        int archived = 0;
        int skipped = 0;
        for (Long id : uniqueIds) {
            try {
                GuideModels.GuideDetail before = adminDetail(id);
                boolean alreadyArchived = "ARCHIVED".equals(before.guide().status());
                jdbc.update("UPDATE game_guides SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?", id);
                audit.log("GUIDE_ARCHIVE", "GAME_GUIDE", id.toString(), operator, before.guide(), null, reason, request);
                if (alreadyArchived) {
                    skipped++;
                } else {
                    archived++;
                }
            } catch (NotFoundException ignored) {
                skipped++;
            }
        }
        return new GuideModels.GuideBatchArchiveResult(archived, skipped);
    }

    @Transactional
    public GuideModels.GuideBatchArchiveResult archiveAll(String reason, String operator, HttpServletRequest request) {
        List<Long> ids = adminList().stream().map(GuideModels.GuideSummary::id).toList();
        if (ids.isEmpty()) {
            return new GuideModels.GuideBatchArchiveResult(0, 0);
        }
        return archiveBatch(ids, reason, operator, request);
    }

    @Transactional
    public GuideModels.GuideImportResult importGames(GuideModels.GuideImportRequest request, String operator, HttpServletRequest httpRequest) {
        boolean publish = request == null || request.publish() == null || request.publish();
        boolean refresh = request != null && Boolean.TRUE.equals(request.refresh());
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "按游戏与服务目录生成多游戏攻略内容"
                : request.reason().trim();
        List<Map<String, Object>> games = jdbc.queryForList("""
                SELECT gc.code, gc.name, gc.name_en, gc.description, gc.genres, gc.cover_image,
                       gc.publisher, gc.seo_slug, gc.business_status
                FROM game_catalog gc
                WHERE gc.is_active = 1 OR EXISTS (SELECT 1 FROM services s WHERE s.game_code = gc.code AND s.is_active = 1)
                ORDER BY gc.sort_order ASC, gc.code ASC
                """);
        int created = 0;
        int refreshed = 0;
        int skipped = 0;
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> game : games) {
            String gameCode = string(game.get("code"));
            List<Map<String, Object>> services = jdbc.queryForList("""
                    SELECT s.id, s.slug, s.name, s.category, s.short_desc, s.price_mode, s.price_fixed, s.price_min, s.price_max,
                           s.duration_text, s.cover_image, s.hot_score, s.sort_order, s.manual_priority
                    FROM services s
                    JOIN game_catalog gc ON gc.code = s.game_code
                    JOIN service_categories sc ON sc.game_code = s.game_code AND sc.code = s.category
                    WHERE s.game_code = ? AND s.is_active = 1 AND gc.is_active = 1 AND sc.is_active = 1
                      AND s.status <> 'OFFLINE'
                      AND (s.published_at IS NULL OR s.published_at <= NOW())
                      AND (s.expires_at IS NULL OR s.expires_at > NOW())
                    ORDER BY (s.hot_score * 10 + s.manual_priority) DESC, s.sort_order ASC, s.id ASC
                    """, gameCode);
            if (services.isEmpty()) {
                skipped++;
                continue;
            }
            Map<String, Object> existing = one("SELECT id, slug FROM game_guides WHERE game_code = ? ORDER BY id ASC LIMIT 1", gameCode);
            if (existing != null && !refresh) {
                skipped++;
                continue;
            }
            String slug = existing == null ? uniqueGuideSlug(slugify(gameCode) + "-guide") : string(existing.get("slug"));
            GuideModels.GuideUpsert generated = generatedGuide(game, services, slug, publish, reason);
            if (existing == null) {
                GuideModels.GuideDetail detail = create(generated, operator, httpRequest);
                ids.add(detail.guide().id());
                created++;
            } else {
                Long id = longObject(existing.get("id"));
                GuideModels.GuideDetail detail = update(id, generated, operator, httpRequest);
                ids.add(detail.guide().id());
                refreshed++;
            }
        }
        return new GuideModels.GuideImportResult(games.size(), created, refreshed, skipped, ids);
    }

    private GuideModels.GuideUpsert generatedGuide(Map<String, Object> game, List<Map<String, Object>> services, String slug, boolean publish, String reason) {
        String gameCode = string(game.get("code"));
        String gameName = string(game.get("name"));
        String description = string(game.get("description"));
        String publisher = string(game.get("publisher"));
        List<String> serviceNames = services.stream().map(x -> string(x.get("name"))).filter(x -> !x.isBlank()).toList();
        List<String> tags = splitTags(string(game.get("genres")));
        tags.addAll(serviceNames.stream().limit(4).toList());
        List<String> uniqueTags = tags.stream().filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().limit(8).toList();
        String summary = (description == null || description.isBlank())
                ? gameName + " 当前可接服务与任务路径汇总。按账号当前进度与目标报价，网页不收取账号密码、验证码或令牌。"
                : description.trim() + " 本攻略汇总当前可接服务、推进顺序与安全交付注意事项。";
        List<GuideModels.SectionInput> sections = List.of(
                new GuideModels.SectionInput("service-map", gameName + " 当前可接服务", serviceMapBody(gameName, services), null, "1", 10),
                new GuideModels.SectionInput("route", "推荐推进顺序", routeBody(gameName, services), null, null, 20),
                new GuideModels.SectionInput("safety", "交付与账号安全", "网页只收集需求、截图与联系方式，不收账号密码、验证码或令牌。\n\n客服确认金额后，可选择淘宝或闲鱼担保；已开放支付的官网订单可在个人中心使用微信扫码。开工前请核对区服、平台、目标、可操作时间和异常处理方式；确认不满足条件时，不承诺立即履约。\n\n如果需要分阶段推进，可以先把当前截图或进度说明发客服，由客服按阶段拆分任务，避免一次承诺过多内容。", null, null, 30),
                new GuideModels.SectionInput("checklist", "下单前检查清单", "1. 确认游戏区服与登录平台，例如 Steam、Battle.net、PlayStation、Xbox、移动端或国服。\n2. 确认当前进度和目标进度，最好准备角色、段位、任务或库存截图。\n3. 说明每天可操作时段和是否允许组队，减少等待与冲突。\n4. 如涉及活动截止、赛季结算或周赛资格，先确认剩余时间是否足够。\n5. 价格以客服确认的最终金额为准，确认前不要支付。", null, null, 40)
        );
        List<GuideModels.ProductInput> products = new ArrayList<>();
        int order = 10;
        for (Map<String, Object> service : services) {
            products.add(new GuideModels.ProductInput(
                    longObject(service.get("id")),
                    string(service.get("name")),
                    string(service.get("short_desc")),
                    null,
                    order <= 20,
                    order
            ));
            order += 10;
        }
        List<GuideModels.FaqInput> faqs = List.of(
                new GuideModels.FaqInput("现在可以直接下单吗？", "页面会显示当前运营状态。处于预约或可预约状态时，可以先提交需求；客服确认报价、区服与时间后才安排开工。", 10),
                new GuideModels.FaqInput("需要提供账号密码吗？", "不需要。攻略页和询价页都不要求账号密码、验证码或登录令牌；具体上号或组队方式只在客服确认后单独沟通。", 20),
                new GuideModels.FaqInput("价格怎么计算？", "按当前进度到目标进度的差距、预计耗时和活动截止时间综合报价。网页展示的参考价不是最终结算价，最终以客服确认金额为准。", 30)
        );
        String difficulty = services.stream().anyMatch(x -> Set.of("RANKED", "CHAMPION", "EVENT", "CAMO", "POWER").contains(string(x.get("category")).toUpperCase(Locale.ROOT)))
                ? "INTERMEDIATE" : "BEGINNER";
        String status = publish ? "PUBLISHED" : "DRAFT";
        String cover = string(game.get("cover_image"));
        String canonical = "https://wydlgf.cn/guides/" + slug;
        return new GuideModels.GuideUpsert(
                gameCode,
                slug,
                gameName + " 服务攻略与任务路径",
                gameName + " 多服务对比 · 推进顺序 · 安全交付",
                summary,
                cover,
                difficulty,
                60,
                "zh-CN",
                "星火攻略组",
                publisher,
                null,
                status,
                true,
                100,
                uniqueTags,
                gameName + " 代练服务攻略 | 星火网游代练平台",
                summary,
                canonical,
                sections,
                products,
                faqs,
                List.of(),
                reason
        );
    }

    private String serviceMapBody(String gameName, List<Map<String, Object>> services) {
        StringBuilder out = new StringBuilder();
        out.append("截至当前，").append(gameName).append("可接服务包括：\n\n");
        for (Map<String, Object> service : services) {
            out.append("- **").append(string(service.get("name"))).append("**：")
                    .append(string(service.get("short_desc"))).append("（")
                    .append(string(service.get("duration_text"))).append("）\n");
        }
        out.append("\n不同服务的计价方式不同：固定价适合明确目标，区间价适合按当前进度估算，最终以客服确认的业务单为准。");
        return out.toString();
    }

    private String routeBody(String gameName, List<Map<String, Object>> services) {
        String first = services.isEmpty() ? "当前服务" : string(services.get(0).get("name"));
        String second = services.size() < 2 ? "后续目标" : string(services.get(1).get("name"));
        return "推荐先处理会影响后续效率的基础进度，再进入高价值目标。\n\n"
                + "1. 先确认 " + gameName + " 当前账号状态、可用平台与目标是否可执行。\n"
                + "2. 优先安排「" + first + "」，把基础进度或通行证类内容补齐。\n"
                + "3. 再评估「" + second + "」等目标，避免因为活动截止或段位波动造成重复报价。\n"
                + "4. 如果目标是排行榜、周赛资格或限时挑战，先提交截止时间和当前截图，由客服判断是否需要加急。";
    }
    private GuideModels.GuideUpsert validateAndNormalize(GuideModels.GuideUpsert input) {
        if (input == null) {
            throw new BadRequestException("攻略内容不能为空");
        }
        String gameCode = required(input.gameCode(), "请选择游戏").trim().toUpperCase(Locale.ROOT);
        Map<String, Object> game = one("SELECT code FROM game_catalog WHERE code = ?", gameCode);
        if (game == null) {
            throw new BadRequestException("游戏不存在");
        }
        String title = required(input.title(), "请填写攻略标题").trim();
        String slug = slugify(input.slug() == null || input.slug().isBlank() ? title : input.slug());
        if (slug.isBlank()) {
            slug = slugify(gameCode + "-guide");
        }
        String status = normalizeStatus(input.status() == null || input.status().isBlank() ? "DRAFT" : input.status());
        String difficulty = input.difficulty() == null || input.difficulty().isBlank() ? "BEGINNER" : input.difficulty().trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(difficulty)) {
            throw new BadRequestException("非法难度等级");
        }
        List<GuideModels.SectionInput> sections = input.sections() == null ? List.of() : input.sections().stream()
                .filter(Objects::nonNull)
                .filter(x -> !(x.heading() == null || x.heading().isBlank()))
                .map(x -> new GuideModels.SectionInput(
                        x.sectionKey() == null || x.sectionKey().isBlank() ? slugify(x.heading()) : slugify(x.sectionKey()),
                        x.heading().trim(),
                        x.bodyMarkdown() == null ? "" : x.bodyMarkdown().trim(),
                        trimNull(x.imageUrl()),
                        trimNull(x.imageAlt()),
                        x.sortOrder() == null ? 10 : x.sortOrder()))
                .toList();
        if ("PUBLISHED".equals(status) && sections.isEmpty()) {
            throw new BadRequestException("发布攻略前至少需要一个内容段落");
        }
        List<GuideModels.ProductInput> products = input.products() == null ? List.of() : input.products().stream()
                .filter(Objects::nonNull)
                .filter(x -> x.serviceId() != null)
                .map(x -> new GuideModels.ProductInput(
                        x.serviceId(),
                        trimNull(x.label()),
                        trimNull(x.description()),
                        trimNull(x.imageUrl()),
                        x.featured() != null && x.featured(),
                        x.sortOrder() == null ? 10 : x.sortOrder()))
                .toList();
        for (GuideModels.ProductInput product : products) {
            Map<String, Object> service = one("SELECT game_code FROM services WHERE id = ?", product.serviceId());
            if (service == null || !gameCode.equals(string(service.get("game_code")))) {
                throw new BadRequestException("推荐商品不属于当前游戏");
            }
        }
        List<GuideModels.FaqInput> faqs = input.faqs() == null ? List.of() : input.faqs().stream()
                .filter(Objects::nonNull)
                .filter(x -> !(x.question() == null || x.question().isBlank()) && !(x.answer() == null || x.answer().isBlank()))
                .map(x -> new GuideModels.FaqInput(x.question().trim(), x.answer().trim(), x.sortOrder() == null ? 10 : x.sortOrder()))
                .toList();
        List<String> tags = input.tags() == null ? List.of() : input.tags().stream().filter(Objects::nonNull).map(String::trim).filter(x -> !x.isBlank()).distinct().limit(12).toList();
        List<Long> relatedIds = input.relatedGuideIds() == null ? List.of() : input.relatedGuideIds().stream().filter(Objects::nonNull).distinct().limit(12).toList();
        return new GuideModels.GuideUpsert(
                gameCode,
                slug,
                title,
                trimNull(input.subtitle()),
                required(input.summary(), "请填写攻略摘要").trim(),
                trimNull(input.coverImage()),
                difficulty,
                input.estimatedMinutes() == null ? 30 : Math.max(1, Math.min(input.estimatedMinutes(), 10080)),
                input.language() == null || input.language().isBlank() ? "zh-CN" : input.language().trim(),
                input.authorName() == null || input.authorName().isBlank() ? "星火攻略组" : input.authorName().trim(),
                trimNull(input.sourceName()),
                trimNull(input.sourceUrl()),
                status,
                input.featured() != null && input.featured(),
                input.sortOrder() == null ? 100 : Math.max(0, input.sortOrder()),
                tags,
                trimNull(input.seoTitle()),
                trimNull(input.seoDescription()),
                trimNull(input.canonicalUrl()),
                sections,
                products,
                faqs,
                relatedIds,
                trimNull(input.reason())
        );
    }

    private int bindGuide(PreparedStatement ps, GuideModels.GuideUpsert guide, Timestamp publishedAt, boolean update) throws java.sql.SQLException {
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
        ps.setString(i++, writeTags(guide.tags()));
        ps.setString(i++, guide.seoTitle());
        ps.setString(i++, guide.seoDescription());
        ps.setString(i++, guide.canonicalUrl());
        if (update) {
            ps.setString(i++, guide.status());
            ps.setTimestamp(i++, publishedAt);
        } else if (publishedAt == null) {
            ps.setNull(i++, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(i++, publishedAt);
        }
        return i;
    }

    private void replaceChildren(Long guideId, GuideModels.GuideUpsert guide) {
        jdbc.update("DELETE FROM guide_sections WHERE guide_id = ?", guideId);
        int sectionOrder = 10;
        for (GuideModels.SectionInput section : guide.sections()) {
            jdbc.update("""
                    INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, guideId, section.sectionKey(), section.heading(), section.bodyMarkdown(), section.imageUrl(), section.imageAlt(), section.sortOrder() == 0 ? sectionOrder : section.sortOrder());
            sectionOrder += 10;
        }
        jdbc.update("DELETE FROM guide_products WHERE guide_id = ?", guideId);
        int productOrder = 10;
        for (GuideModels.ProductInput product : guide.products()) {
            jdbc.update("""
                    INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, guideId, product.serviceId(), product.label(), product.description(), product.imageUrl(), product.featured(), product.sortOrder() == 0 ? productOrder : product.sortOrder());
            productOrder += 10;
        }
        jdbc.update("DELETE FROM guide_faqs WHERE guide_id = ?", guideId);
        int faqOrder = 10;
        for (GuideModels.FaqInput faq : guide.faqs()) {
            jdbc.update("""
                    INSERT INTO guide_faqs (guide_id, question, answer, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, guideId, faq.question(), faq.answer(), faq.sortOrder() == 0 ? faqOrder : faq.sortOrder());
            faqOrder += 10;
        }
        jdbc.update("DELETE FROM guide_relations WHERE guide_id = ?", guideId);
        for (Long relatedId : guide.relatedGuideIds()) {
            if (relatedId != null && !relatedId.equals(guideId) && jdbc.queryForObject("SELECT COUNT(*) FROM game_guides WHERE id = ?", Integer.class, relatedId) > 0) {
                jdbc.update("INSERT INTO guide_relations (guide_id, related_guide_id, sort_order, created_at) VALUES (?, ?, 10, CURRENT_TIMESTAMP)", guideId, relatedId);
            }
        }
    }
    private GuideModels.GuideDetail detail(Map<String, Object> row, boolean publicOnly) {
        GuideModels.GuideSummary guide = summary(row);
        Long id = guide.id();
        List<GuideModels.GuideSection> sections = jdbc.queryForList("""
                SELECT id, section_key, heading, body_markdown, image_url, image_alt, sort_order
                FROM guide_sections WHERE guide_id = ? ORDER BY sort_order ASC, id ASC
                """, id).stream().map(x -> new GuideModels.GuideSection(
                longObject(x.get("id")), string(x.get("section_key")), string(x.get("heading")), string(x.get("body_markdown")),
                string(x.get("image_url")), string(x.get("image_alt")), intValue(x.get("sort_order")))).toList();
        String productSql = """
                SELECT p.service_id, p.label, p.description, p.image_url AS overlay_image, p.is_featured, p.sort_order AS product_sort,
                       s.slug, s.name, s.category, s.short_desc, s.price_mode, s.price_fixed, s.price_min, s.price_max,
                       s.duration_text, s.cover_image, s.is_active
                FROM guide_products p JOIN services s ON s.id = p.service_id
                WHERE p.guide_id = ?
                """ + (publicOnly ? " AND s.is_active = 1" : "") + " ORDER BY p.is_featured DESC, p.sort_order ASC, p.id ASC";
        List<GuideModels.GuideProduct> products = jdbc.queryForList(productSql, id).stream().map(x -> new GuideModels.GuideProduct(
                longObject(x.get("service_id")), string(x.get("slug")), string(x.get("name")), string(x.get("category")),
                string(x.get("short_desc")), string(x.get("price_mode")), decimal(x.get("price_fixed")), decimal(x.get("price_min")),
                decimal(x.get("price_max")), string(x.get("duration_text")), firstNonBlank(string(x.get("overlay_image")), string(x.get("cover_image"))),
                string(x.get("label")), string(x.get("description")), boolValue(x.get("is_featured")), intValue(x.get("product_sort")))).toList();
        List<GuideModels.GuideFaq> faqs = jdbc.queryForList("""
                SELECT id, question, answer, sort_order FROM guide_faqs WHERE guide_id = ? ORDER BY sort_order ASC, id ASC
                """, id).stream().map(x -> new GuideModels.GuideFaq(longObject(x.get("id")), string(x.get("question")), string(x.get("answer")), intValue(x.get("sort_order")))).toList();
        List<GuideModels.GuideRelated> related = jdbc.queryForList("""
                SELECT g2.id, g2.game_code, gc.name AS game_name, g2.slug, g2.title, g2.cover_image
                FROM guide_relations r
                JOIN game_guides g2 ON g2.id = r.related_guide_id
                JOIN game_catalog gc ON gc.code = g2.game_code
                WHERE r.guide_id = ? AND g2.status = 'PUBLISHED'
                ORDER BY r.sort_order ASC, g2.id DESC
                """, id).stream().map(x -> new GuideModels.GuideRelated(
                longObject(x.get("id")), string(x.get("game_code")), string(x.get("game_name")), string(x.get("slug")),
                string(x.get("title")), string(x.get("cover_image")))).toList();
        return new GuideModels.GuideDetail(guide, sections, products, faqs, related);
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

    private GuideModels.GuideSummary summary(Map<String, Object> row) {
        return new GuideModels.GuideSummary(
                longObject(row.get("id")), string(row.get("game_code")), string(row.get("game_name")), string(row.get("slug")),
                string(row.get("title")), string(row.get("subtitle")), string(row.get("summary")), string(row.get("cover_image")),
                string(row.get("difficulty")), intValue(row.get("estimated_minutes")), string(row.get("language")),
                string(row.get("author_name")), string(row.get("source_name")), string(row.get("source_url")),
                string(row.get("status")), boolValue(row.get("is_featured")), intValue(row.get("sort_order")),
                readTags(string(row.get("tags_json"))), string(row.get("seo_title")), string(row.get("seo_description")),
                string(row.get("canonical_url")), longValue(row.get("view_count")), instant(row.get("published_at")),
                instant(row.get("updated_at")), intValue(row.get("section_count")), intValue(row.get("product_count"))
        );
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String uniqueGuideSlug(String base) {
        String candidate = base;
        int suffix = 2;
        while (jdbc.queryForObject("SELECT COUNT(*) FROM game_guides WHERE slug = ?", Integer.class, candidate) > 0) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String out = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return out.length() > 120 ? out.substring(0, 120).replaceAll("-+$", "") : out;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "DRAFT" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new BadRequestException("非法攻略状态");
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
            return mapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (Exception e) {
            return "[]";
        }
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

    private List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(raw.split("[,，/|、]+")).map(String::trim).filter(x -> !x.isBlank()).toList());
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
