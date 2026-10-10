package com.xinghuo.guide.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.audit.infrastructure.AuditService;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.guide.application.GuideQueryModels;
import com.xinghuo.guide.application.GuideQueryPort;
import com.xinghuo.guide.application.GuideWriteModels;
import com.xinghuo.guide.application.GuideWritePort;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Guide-owned JDBC mutation, validation, import, and audit adapter. */
@Component
public class GuideWriteJdbcAdapter implements GuideWritePort {
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Set<String> DIFFICULTIES = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT");

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AuditService audit;
    private final GuideQueryPort queries;

    public GuideWriteJdbcAdapter(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            AuditService audit,
            GuideQueryPort queries) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.audit = audit;
        this.queries = queries;
    }

    @Override
    @Transactional
    public GuideQueryModels.GuideDetail create(GuideWriteModels.GuideUpsertCommand input, String operator) {
        GuideWriteModels.GuideUpsertCommand normalized = validateAndNormalize(input);
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
        GuideQueryModels.GuideDetail after = queries.adminDetail(id);
        audit.log(
                "GUIDE_CREATE",
                "GAME_GUIDE",
                id.toString(),
                operator,
                null,
                after.guide(),
                normalized.reason(),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public GuideQueryModels.GuideDetail update(
            Long id, GuideWriteModels.GuideUpsertCommand input, String operator) {
        GuideQueryModels.GuideDetail before = queries.adminDetail(id);
        GuideWriteModels.GuideUpsertCommand normalized = validateAndNormalize(input);
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
        GuideQueryModels.GuideDetail after = queries.adminDetail(id);
        audit.log(
                "GUIDE_UPDATE",
                "GAME_GUIDE",
                id.toString(),
                operator,
                before.guide(),
                after.guide(),
                normalized.reason(),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public GuideQueryModels.GuideDetail setStatus(Long id, String status, String reason, String operator) {
        String normalizedStatus = normalizeStatus(status);
        GuideQueryModels.GuideDetail before = queries.adminDetail(id);
        int changed;
        if ("PUBLISHED".equals(normalizedStatus)) {
            changed = jdbc.update(
                    "UPDATE game_guides SET status=?, published_at=COALESCE(published_at, CURRENT_TIMESTAMP), updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?",
                    normalizedStatus,
                    id);
        } else {
            changed = jdbc.update(
                    "UPDATE game_guides SET status=?, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?",
                    normalizedStatus,
                    id);
        }
        if (changed == 0) {
            throw new NotFoundException();
        }
        GuideQueryModels.GuideDetail after = queries.adminDetail(id);
        audit.log(
                "GUIDE_STATUS",
                "GAME_GUIDE",
                id.toString(),
                operator,
                before.guide(),
                after.guide(),
                reason,
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public void archive(Long id, String reason, String operator) {
        GuideQueryModels.GuideDetail before = queries.adminDetail(id);
        jdbc.update(
                "UPDATE game_guides SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?",
                id);
        audit.log(
                "GUIDE_ARCHIVE",
                "GAME_GUIDE",
                id.toString(),
                operator,
                before.guide(),
                null,
                reason,
                currentRequest());
    }

    @Override
    @Transactional
    public GuideWriteModels.GuideBatchArchiveResult archiveBatch(List<Long> ids, String reason, String operator) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("请选择要归档的攻略");
        }
        List<Long> uniqueIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        int archived = 0;
        int skipped = 0;
        for (Long id : uniqueIds) {
            try {
                GuideQueryModels.GuideDetail before = queries.adminDetail(id);
                boolean alreadyArchived = "ARCHIVED".equals(before.guide().status());
                jdbc.update(
                        "UPDATE game_guides SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=?",
                        id);
                audit.log(
                        "GUIDE_ARCHIVE",
                        "GAME_GUIDE",
                        id.toString(),
                        operator,
                        before.guide(),
                        null,
                        reason,
                        currentRequest());
                if (alreadyArchived) {
                    skipped++;
                } else {
                    archived++;
                }
            } catch (NotFoundException ignored) {
                skipped++;
            }
        }
        return new GuideWriteModels.GuideBatchArchiveResult(archived, skipped);
    }

    @Override
    @Transactional
    public GuideWriteModels.GuideBatchArchiveResult archiveAll(String reason, String operator) {
        List<Long> ids = queries.adminList().stream().map(GuideQueryModels.GuideSummary::id).toList();
        if (ids.isEmpty()) {
            return new GuideWriteModels.GuideBatchArchiveResult(0, 0);
        }
        return archiveBatch(ids, reason, operator);
    }

    @Override
    @Transactional
    public GuideWriteModels.GuideImportResult importGames(GuideWriteModels.GuideImportCommand request, String operator) {
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
            Map<String, Object> existing = one(
                    "SELECT id, slug FROM game_guides WHERE game_code = ? ORDER BY id ASC LIMIT 1",
                    gameCode);
            if (existing != null && !refresh) {
                skipped++;
                continue;
            }
            String slug = existing == null
                    ? uniqueGuideSlug(slugify(gameCode) + "-guide")
                    : string(existing.get("slug"));
            GuideWriteModels.GuideUpsertCommand generated = generatedGuide(game, services, slug, publish, reason);
            if (existing == null) {
                GuideQueryModels.GuideDetail detail = create(generated, operator);
                ids.add(detail.guide().id());
                created++;
            } else {
                Long id = longObject(existing.get("id"));
                GuideQueryModels.GuideDetail detail = update(id, generated, operator);
                ids.add(detail.guide().id());
                refreshed++;
            }
        }
        return new GuideWriteModels.GuideImportResult(games.size(), created, refreshed, skipped, ids);
    }

    private GuideWriteModels.GuideUpsertCommand generatedGuide(
            Map<String, Object> game,
            List<Map<String, Object>> services,
            String slug,
            boolean publish,
            String reason) {
        String gameCode = string(game.get("code"));
        String gameName = string(game.get("name"));
        String description = string(game.get("description"));
        String publisher = string(game.get("publisher"));
        List<String> serviceNames = services.stream()
                .map(x -> string(x.get("name")))
                .filter(x -> !x.isBlank())
                .toList();
        List<String> tags = splitTags(string(game.get("genres")));
        tags.addAll(serviceNames.stream().limit(4).toList());
        List<String> uniqueTags = tags.stream()
                .filter(x -> x != null && !x.isBlank())
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
        String summary = (description == null || description.isBlank())
                ? gameName + " 当前可接服务与任务路径汇总。按账号当前进度与目标报价，网页不收取账号密码、验证码或令牌。"
                : description.trim() + " 本攻略汇总当前可接服务、推进顺序与安全交付注意事项。";
        List<GuideWriteModels.SectionInput> sections = List.of(
                new GuideWriteModels.SectionInput(
                        "service-map", gameName + " 当前可接服务", serviceMapBody(gameName, services), null, "1", 10),
                new GuideWriteModels.SectionInput(
                        "route", "推荐推进顺序", routeBody(gameName, services), null, null, 20),
                new GuideWriteModels.SectionInput(
                        "safety",
                        "交付与账号安全",
                        "网页只收集需求、截图与联系方式，不收账号密码、验证码或令牌。\n\n确认报价后走淘宝或闲鱼担保交易。开工前请核对区服、平台、目标、可操作时间和异常处理方式；确认不满足条件时，不承诺立即履约。\n\n如果需要分阶段推进，可以先把当前截图或进度说明发客服，由客服按阶段拆分任务，避免一次承诺过多内容。",
                        null,
                        null,
                        30),
                new GuideWriteModels.SectionInput(
                        "checklist",
                        "下单前检查清单",
                        "1. 确认游戏区服与登录平台，例如 Steam、Battle.net、PlayStation、Xbox、移动端或国服。\n2. 确认当前进度和目标进度，最好准备角色、段位、任务或库存截图。\n3. 说明每天可操作时段和是否允许组队，减少等待与冲突。\n4. 如涉及活动截止、赛季结算或周赛资格，先确认剩余时间是否足够。\n5. 价格以人工确认后的担保订单为准，确认前不要支付。",
                        null,
                        null,
                        40));
        List<GuideWriteModels.ProductInput> products = new ArrayList<>();
        int order = 10;
        for (Map<String, Object> service : services) {
            products.add(new GuideWriteModels.ProductInput(
                    longObject(service.get("id")),
                    string(service.get("name")),
                    string(service.get("short_desc")),
                    null,
                    order <= 20,
                    order));
            order += 10;
        }
        List<GuideWriteModels.FaqInput> faqs = List.of(
                new GuideWriteModels.FaqInput(
                        "现在可以直接下单吗？",
                        "页面会显示当前运营状态。处于预约或可预约状态时，可以先提交需求；客服确认报价、区服与时间后才安排开工。",
                        10),
                new GuideWriteModels.FaqInput(
                        "需要提供账号密码吗？",
                        "不需要。攻略页和询价页都不要求账号密码、验证码或登录令牌；具体上号或组队方式只在客服确认后单独沟通。",
                        20),
                new GuideWriteModels.FaqInput(
                        "价格怎么计算？",
                        "按当前进度到目标进度的差距、预计耗时和活动截止时间综合报价。网页展示的参考价不是最终结算价，最终以担保订单确认金额为准。",
                        30));
        String difficulty = services.stream().anyMatch(x -> Set.of("RANKED", "CHAMPION", "EVENT", "CAMO", "POWER")
                        .contains(string(x.get("category")).toUpperCase(Locale.ROOT)))
                ? "INTERMEDIATE"
                : "BEGINNER";
        String status = publish ? "PUBLISHED" : "DRAFT";
        String cover = string(game.get("cover_image"));
        String canonical = "https://wydlgf.cn/guides/" + slug;
        return new GuideWriteModels.GuideUpsertCommand(
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
                reason);
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

    private GuideWriteModels.GuideUpsertCommand validateAndNormalize(GuideWriteModels.GuideUpsertCommand input) {
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
        String difficulty = input.difficulty() == null || input.difficulty().isBlank()
                ? "BEGINNER"
                : input.difficulty().trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(difficulty)) {
            throw new BadRequestException("非法难度等级");
        }
        List<GuideWriteModels.SectionInput> sections = input.sections() == null
                ? List.of()
                : input.sections().stream()
                        .filter(Objects::nonNull)
                        .filter(x -> !(x.heading() == null || x.heading().isBlank()))
                        .map(x -> new GuideWriteModels.SectionInput(
                                x.sectionKey() == null || x.sectionKey().isBlank()
                                        ? slugify(x.heading())
                                        : slugify(x.sectionKey()),
                                x.heading().trim(),
                                x.bodyMarkdown() == null ? "" : x.bodyMarkdown().trim(),
                                trimNull(x.imageUrl()),
                                trimNull(x.imageAlt()),
                                x.sortOrder() == null ? 10 : x.sortOrder()))
                        .toList();
        if ("PUBLISHED".equals(status) && sections.isEmpty()) {
            throw new BadRequestException("发布攻略前至少需要一个内容段落");
        }
        List<GuideWriteModels.ProductInput> products = input.products() == null
                ? List.of()
                : input.products().stream()
                        .filter(Objects::nonNull)
                        .filter(x -> x.serviceId() != null)
                        .map(x -> new GuideWriteModels.ProductInput(
                                x.serviceId(),
                                trimNull(x.label()),
                                trimNull(x.description()),
                                trimNull(x.imageUrl()),
                                x.featured() != null && x.featured(),
                                x.sortOrder() == null ? 10 : x.sortOrder()))
                        .toList();
        for (GuideWriteModels.ProductInput product : products) {
            Map<String, Object> service = one("SELECT game_code FROM services WHERE id = ?", product.serviceId());
            if (service == null || !gameCode.equals(string(service.get("game_code")))) {
                throw new BadRequestException("推荐商品不属于当前游戏");
            }
        }
        List<GuideWriteModels.FaqInput> faqs = input.faqs() == null
                ? List.of()
                : input.faqs().stream()
                        .filter(Objects::nonNull)
                        .filter(x -> !(x.question() == null || x.question().isBlank())
                                && !(x.answer() == null || x.answer().isBlank()))
                        .map(x -> new GuideWriteModels.FaqInput(
                                x.question().trim(),
                                x.answer().trim(),
                                x.sortOrder() == null ? 10 : x.sortOrder()))
                        .toList();
        List<String> tags = input.tags() == null
                ? List.of()
                : input.tags().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(x -> !x.isBlank())
                        .distinct()
                        .limit(12)
                        .toList();
        List<Long> relatedIds = input.relatedGuideIds() == null
                ? List.of()
                : input.relatedGuideIds().stream().filter(Objects::nonNull).distinct().limit(12).toList();
        return new GuideWriteModels.GuideUpsertCommand(
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
                trimNull(input.reason()));
    }

    private int bindGuide(
            PreparedStatement ps,
            GuideWriteModels.GuideUpsertCommand guide,
            Timestamp publishedAt,
            boolean update) throws java.sql.SQLException {
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

    private void replaceChildren(Long guideId, GuideWriteModels.GuideUpsertCommand guide) {
        jdbc.update("DELETE FROM guide_sections WHERE guide_id = ?", guideId);
        int sectionOrder = 10;
        for (GuideWriteModels.SectionInput section : guide.sections()) {
            jdbc.update("""
                    INSERT INTO guide_sections (guide_id, section_key, heading, body_markdown, image_url, image_alt, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    guideId,
                    section.sectionKey(),
                    section.heading(),
                    section.bodyMarkdown(),
                    section.imageUrl(),
                    section.imageAlt(),
                    section.sortOrder() == 0 ? sectionOrder : section.sortOrder());
            sectionOrder += 10;
        }
        jdbc.update("DELETE FROM guide_products WHERE guide_id = ?", guideId);
        int productOrder = 10;
        for (GuideWriteModels.ProductInput product : guide.products()) {
            jdbc.update("""
                    INSERT INTO guide_products (guide_id, service_id, label, description, image_url, is_featured, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    guideId,
                    product.serviceId(),
                    product.label(),
                    product.description(),
                    product.imageUrl(),
                    product.featured(),
                    product.sortOrder() == 0 ? productOrder : product.sortOrder());
            productOrder += 10;
        }
        jdbc.update("DELETE FROM guide_faqs WHERE guide_id = ?", guideId);
        int faqOrder = 10;
        for (GuideWriteModels.FaqInput faq : guide.faqs()) {
            jdbc.update("""
                    INSERT INTO guide_faqs (guide_id, question, answer, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    guideId,
                    faq.question(),
                    faq.answer(),
                    faq.sortOrder() == 0 ? faqOrder : faq.sortOrder());
            faqOrder += 10;
        }
        jdbc.update("DELETE FROM guide_relations WHERE guide_id = ?", guideId);
        for (Long relatedId : guide.relatedGuideIds()) {
            if (relatedId != null
                    && !relatedId.equals(guideId)
                    && jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM game_guides WHERE id = ?", Integer.class, relatedId)
                            > 0) {
                jdbc.update(
                        "INSERT INTO guide_relations (guide_id, related_guide_id, sort_order, created_at) VALUES (?, ?, 10, CURRENT_TIMESTAMP)",
                        guideId,
                        relatedId);
            }
        }
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
        String out = value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT)
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

    private List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(raw.split("[,，/|、]+"))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .toList());
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long longObject(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
