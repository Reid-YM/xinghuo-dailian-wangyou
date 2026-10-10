package com.xinghuo.taxonomy.infrastructure;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.GamePlatform;
import com.xinghuo.model.ServiceCategoryCatalog;
import com.xinghuo.repository.GameCatalogRepository;
import com.xinghuo.repository.GamePlatformRepository;
import com.xinghuo.repository.PlatformCatalogRepository;
import com.xinghuo.repository.ServiceCategoryCatalogRepository;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.taxonomy.application.TaxonomyCatalogView;
import com.xinghuo.taxonomy.application.TaxonomyCommands;
import com.xinghuo.taxonomy.application.TaxonomyPort;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Taxonomy-owned persistence, visibility-cache, and audit adapter. */
@Component
public class TaxonomyPersistenceAdapter implements TaxonomyPort {
    private static final Set<String> ICON_KEYS = Set.of(
            "gamepad", "event", "weapon", "camo", "rank", "service", "quote", "order",
            "record", "support", "settings", "audit", "desktop", "console", "mobile",
            "library", "search", "star");
    private static final Set<String> BUSINESS_STATUSES = Set.of("ACTIVE", "READY", "PLANNED", "PAUSED");
    private static final long ALLOWED_PAIRS_TTL_MS = 60_000L;

    private final GameCatalogRepository games;
    private final ServiceCategoryCatalogRepository categories;
    private final ServiceItemRepository services;
    private final PlatformCatalogRepository platforms;
    private final GamePlatformRepository gamePlatforms;
    private final AuditService audit;

    private volatile Set<String> allowedPairs;
    private volatile long allowedPairsLoadedAt;

    public TaxonomyPersistenceAdapter(
            GameCatalogRepository games,
            ServiceCategoryCatalogRepository categories,
            ServiceItemRepository services,
            PlatformCatalogRepository platforms,
            GamePlatformRepository gamePlatforms,
            AuditService audit) {
        this.games = games;
        this.categories = categories;
        this.services = services;
        this.platforms = platforms;
        this.gamePlatforms = gamePlatforms;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxonomyCatalogView catalog(boolean admin) {
        List<GamePlatform> allLinks = gamePlatforms.findAllByOrderByGameCodeAscSortOrderAscIdAsc();
        HashMap<String, List<String>> platformCodesByGame = new HashMap<>();
        for (GamePlatform link : allLinks) {
            if (!link.isActive()) {
                continue;
            }
            platformCodesByGame
                    .computeIfAbsent(link.getGameCode(), ignored -> new ArrayList<>())
                    .add(link.getPlatformCode());
        }

        List<TaxonomyCatalogView.GameView> gameViews = games.findAllByOrderBySortOrderAscCodeAsc().stream()
                .filter(game -> admin || game.isActive())
                .map(game -> gameView(game, platformCodesByGame.getOrDefault(game.getCode(), List.of())))
                .toList();

        HashMap<String, Long> serviceCountByCategory = new HashMap<>();
        for (Object[] row : services.countGroupedByGameAndCategory()) {
            serviceCountByCategory.put(
                    String.valueOf(row[0]) + "|" + String.valueOf(row[1]),
                    ((Number) row[2]).longValue());
        }

        List<TaxonomyCatalogView.CategoryView> categoryViews =
                categories.findAllByOrderBySortOrderAscIdAsc().stream()
                        .filter(category -> admin
                                || category.isActive()
                                && games.findById(category.getGameCode())
                                        .map(GameCatalog::isActive)
                                        .orElse(false))
                        .map(category -> categoryView(
                                category,
                                serviceCountByCategory.getOrDefault(
                                        category.getGameCode() + "|" + category.getCode(), 0L)))
                        .toList();

        List<TaxonomyCatalogView.PlatformView> platformViews =
                platforms.findAllByOrderBySortOrderAscCodeAsc().stream()
                        .filter(platform -> admin || platform.isActive())
                        .map(platform -> new TaxonomyCatalogView.PlatformView(
                                platform.getCode(),
                                platform.getName(),
                                icon(platform.getIconKey(), platformIcon(platform.getPlatformGroup())),
                                platform.getPlatformGroup(),
                                platform.getDescription(),
                                platform.isActive(),
                                platform.getSortOrder()))
                        .toList();

        Set<String> visibleGames = gameViews.stream()
                .map(TaxonomyCatalogView.GameView::code)
                .collect(Collectors.toSet());
        Set<String> visiblePlatforms = platformViews.stream()
                .map(TaxonomyCatalogView.PlatformView::code)
                .collect(Collectors.toSet());
        List<TaxonomyCatalogView.GamePlatformView> gamePlatformViews = allLinks.stream()
                .filter(link -> admin
                        || link.isActive()
                        && visibleGames.contains(link.getGameCode())
                        && visiblePlatforms.contains(link.getPlatformCode()))
                .map(link -> new TaxonomyCatalogView.GamePlatformView(
                        link.getGameCode(), link.getPlatformCode(), link.isActive(), link.getSortOrder()))
                .toList();

        return new TaxonomyCatalogView(gameViews, categoryViews, platformViews, gamePlatformViews);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxonomyCatalogView.GameView publicGameBySeoSlug(String seoSlug) {
        GameCatalog game = games.findBySeoSlug(seoSlug).orElseThrow(NotFoundException::new);
        if (!game.isActive()) {
            throw new NotFoundException();
        }
        return gameView(game);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxonomyCatalogView.GameView> publicGames() {
        return games.findAllByOrderBySortOrderAscCodeAsc().stream()
                .filter(GameCatalog::isActive)
                .map(this::gameView)
                .toList();
    }

    @Override
    @Transactional
    public TaxonomyCatalogView.GameView addGame(TaxonomyCommands.CreateGame command, String operator) {
        String code = uniqueGameCode(slug(command.name()));
        Instant now = Instant.now();
        boolean active = command.active() == null || command.active();
        GameCatalog game = GameCatalog.builder()
                .code(code)
                .name(command.name().trim())
                .nameEn(null)
                .aliases(null)
                .iconKey(icon(command.iconKey(), "gamepad"))
                .description(trim(command.description()))
                .publisher(null)
                .genres(null)
                .logoImage(null)
                .coverImage(null)
                .popularityScore(50)
                .featured(false)
                .businessStatus(active ? "ACTIVE" : "PLANNED")
                .seoSlug(code.toLowerCase(Locale.ROOT).replace('_', '-'))
                .seoTitle(null)
                .seoDescription(null)
                .active(active)
                .sortOrder(command.sortOrder() == null ? nextGameSort() : command.sortOrder())
                .createdAt(now)
                .updatedAt(now)
                .build();
        games.save(game);
        TaxonomyCatalogView.GameView after = gameView(game);
        audit.log(
                "GAME_CREATE", "GAME", code, operator, null, after,
                reason(command.reason(), "新增游戏"), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public TaxonomyCatalogView.GameView updateGame(
            String code, TaxonomyCommands.UpdateGame command, String operator) {
        String normalized = normalizeGame(code);
        GameCatalog game = games.findById(normalized).orElseThrow(NotFoundException::new);
        TaxonomyCatalogView.GameView before = gameView(game);
        game.setName(command.name().trim());
        game.setIconKey(icon(command.iconKey(), game.getIconKey() == null ? "gamepad" : game.getIconKey()));
        game.setDescription(trim(command.description()));
        if (command.active() != null) {
            game.setActive(command.active());
        }
        if (command.sortOrder() != null) {
            game.setSortOrder(command.sortOrder());
        }
        game.setUpdatedAt(Instant.now());
        TaxonomyCatalogView.GameView after = gameView(game);
        audit.log(
                "GAME_UPDATE", "GAME", normalized, operator, before, after,
                reason(command.reason(), "更新游戏"), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public TaxonomyCatalogView.GameView updateGameProfile(
            String code, TaxonomyCommands.UpdateGameProfile command, String operator) {
        String normalized = normalizeGame(code);
        GameCatalog game = games.findById(normalized).orElseThrow(NotFoundException::new);
        TaxonomyCatalogView.GameView before = gameView(game);
        game.setName(command.name().trim());
        game.setNameEn(trim(command.nameEn()));
        game.setAliases(trim(command.aliases()));
        game.setIconKey(icon(command.iconKey(), game.getIconKey() == null ? "gamepad" : game.getIconKey()));
        game.setDescription(trim(command.description()));
        game.setPublisher(trim(command.publisher()));
        game.setGenres(trim(command.genres()));
        game.setLogoImage(trim(command.logoImage()));
        game.setCoverImage(trim(command.coverImage()));
        if (command.popularityScore() != null) {
            game.setPopularityScore(command.popularityScore());
        }
        if (command.featured() != null) {
            game.setFeatured(command.featured());
        }
        if (command.businessStatus() != null) {
            game.setBusinessStatus(businessStatus(command.businessStatus()));
        }
        game.setSeoSlug(trim(command.seoSlug()));
        game.setSeoTitle(trim(command.seoTitle()));
        game.setSeoDescription(trim(command.seoDescription()));
        if (command.sortOrder() != null) {
            game.setSortOrder(command.sortOrder());
        }
        if (command.active() != null) {
            game.setActive(command.active());
        }
        game.setUpdatedAt(Instant.now());
        if (command.platformCodes() != null) {
            syncPlatforms(normalized, command.platformCodes());
        }
        TaxonomyCatalogView.GameView after = gameView(game);
        audit.log(
                "GAME_PROFILE_UPDATE", "GAME", normalized, operator, before, after,
                reason(command.reason(), "维护游戏资料中心"), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public List<TaxonomyCatalogView.GameView> bulkUpdateGames(
            TaxonomyCommands.BulkUpdateGames command, String operator) {
        ArrayList<TaxonomyCatalogView.GameView> out = new ArrayList<>();
        for (String raw : command.codes()) {
            String code = normalizeGame(raw);
            GameCatalog game = games.findById(code)
                    .orElseThrow(() -> new BadRequestException("游戏不存在：" + code));
            TaxonomyCatalogView.GameView before = gameView(game);
            if (command.active() != null) {
                game.setActive(command.active());
            }
            if (command.featured() != null) {
                game.setFeatured(command.featured());
            }
            if (command.businessStatus() != null) {
                game.setBusinessStatus(businessStatus(command.businessStatus()));
            }
            if (command.popularityScore() != null) {
                game.setPopularityScore(command.popularityScore());
            }
            game.setUpdatedAt(Instant.now());
            TaxonomyCatalogView.GameView after = gameView(game);
            audit.log(
                    "GAME_BULK_UPDATE", "GAME", code, operator, before, after,
                    reason(command.reason(), "游戏资料批量运营"), currentRequest());
            out.add(after);
        }
        return out;
    }

    @Override
    @Transactional
    public TaxonomyCatalogView.CategoryView addCategory(
            TaxonomyCommands.CreateCategory command, String operator) {
        String gameCode = normalizeGame(command.gameCode());
        requireGameExists(gameCode);
        String code = uniqueCategoryCode(gameCode, slug(command.name()));
        Instant now = Instant.now();
        ServiceCategoryCatalog category = ServiceCategoryCatalog.builder()
                .gameCode(gameCode)
                .code(code)
                .name(command.name().trim())
                .iconKey(icon(command.iconKey(), "service"))
                .description(trim(command.description()))
                .active(command.active() == null || command.active())
                .sortOrder(command.sortOrder() == null ? nextCategorySort(gameCode) : command.sortOrder())
                .createdAt(now)
                .updatedAt(now)
                .build();
        categories.save(category);
        TaxonomyCatalogView.CategoryView after = categoryView(category);
        audit.log(
                "CATEGORY_CREATE", "SERVICE_CATEGORY", gameCode + ":" + code, operator,
                null, after, reason(command.reason(), "新增需求类型"), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public TaxonomyCatalogView.CategoryView updateCategory(
            Long id, TaxonomyCommands.UpdateCategory command, String operator) {
        ServiceCategoryCatalog category = categories.findById(id).orElseThrow(NotFoundException::new);
        TaxonomyCatalogView.CategoryView before = categoryView(category);
        category.setName(command.name().trim());
        category.setIconKey(icon(
                command.iconKey(), category.getIconKey() == null ? "service" : category.getIconKey()));
        category.setDescription(trim(command.description()));
        if (command.active() != null) {
            category.setActive(command.active());
        }
        if (command.sortOrder() != null) {
            category.setSortOrder(command.sortOrder());
        }
        category.setUpdatedAt(Instant.now());
        TaxonomyCatalogView.CategoryView after = categoryView(category);
        audit.log(
                "CATEGORY_UPDATE", "SERVICE_CATEGORY",
                category.getGameCode() + ":" + category.getCode(), operator,
                before, after, reason(command.reason(), "更新需求类型"), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public void deleteGame(String code, String operator) {
        String normalized = normalizeGame(code);
        GameCatalog game = games.findById(normalized).orElseThrow(NotFoundException::new);
        long categoryCount = categories.countByGameCode(normalized);
        long serviceCount = services.countByGameCode(normalized);
        if (categoryCount > 0 || serviceCount > 0) {
            throw new ConflictException("该游戏仍关联需求类型或服务项目，请先处理依赖后再永久删除");
        }
        TaxonomyCatalogView.GameView before = gameView(game);
        gamePlatforms.deleteByGameCode(normalized);
        games.delete(game);
        audit.log(
                "GAME_DELETE", "GAME", normalized, operator, before, null,
                "永久删除无依赖游戏", currentRequest());
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, String operator) {
        ServiceCategoryCatalog category = categories.findById(id).orElseThrow(NotFoundException::new);
        long serviceCount = services.countByGameCodeAndCategory(category.getGameCode(), category.getCode());
        if (serviceCount > 0) {
            throw new ConflictException("该需求类型仍关联服务项目，请先移动或下架并处理依赖后再永久删除");
        }
        TaxonomyCatalogView.CategoryView before = categoryView(category);
        categories.delete(category);
        audit.log(
                "CATEGORY_DELETE", "SERVICE_CATEGORY",
                category.getGameCode() + ":" + category.getCode(), operator, before, null,
                "永久删除无依赖需求类型", currentRequest());
    }

    @Override
    @Transactional(readOnly = true)
    public void validate(String game, String category) {
        String normalizedGame = normalizeGame(game);
        requireGame(normalizedGame);
        categories.findByGameCodeAndCode(
                        normalizedGame,
                        category == null ? "" : category.trim().toUpperCase(Locale.ROOT))
                .filter(ServiceCategoryCatalog::isActive)
                .orElseThrow(() -> new BadRequestException("非法或已停用的二级需求类型"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean publicAllowed(String game, String category) {
        String normalizedGame = normalizeGame(game);
        String normalizedCategory = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        return allowedPairSet().contains(normalizedGame + "|" + normalizedCategory);
    }

    @Override
    public void invalidateVisibilityCache() {
        allowedPairs = null;
        allowedPairsLoadedAt = 0L;
    }

    private Set<String> allowedPairSet() {
        Set<String> cached = allowedPairs;
        long now = System.currentTimeMillis();
        if (cached != null && now - allowedPairsLoadedAt < ALLOWED_PAIRS_TTL_MS) {
            return cached;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (allowedPairs != null && now - allowedPairsLoadedAt < ALLOWED_PAIRS_TTL_MS) {
                return allowedPairs;
            }
            Set<String> activeGames = games.findAllByOrderBySortOrderAscCodeAsc().stream()
                    .filter(GameCatalog::isActive)
                    .map(GameCatalog::getCode)
                    .collect(Collectors.toSet());
            Set<String> pairs = categories.findAllByOrderBySortOrderAscIdAsc().stream()
                    .filter(ServiceCategoryCatalog::isActive)
                    .filter(category -> activeGames.contains(category.getGameCode()))
                    .map(category -> category.getGameCode() + "|" + category.getCode())
                    .collect(Collectors.toSet());
            allowedPairs = pairs;
            allowedPairsLoadedAt = now;
            return pairs;
        }
    }

    private void syncPlatforms(String gameCode, List<String> rawCodes) {
        List<String> codes = rawCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
        for (String code : codes) {
            if (!platforms.existsById(code)) {
                throw new BadRequestException("游戏平台不存在：" + code);
            }
        }
        gamePlatforms.deleteByGameCode(gameCode);
        Instant now = Instant.now();
        int sort = 10;
        for (String code : codes) {
            gamePlatforms.save(GamePlatform.builder()
                    .gameCode(gameCode)
                    .platformCode(code)
                    .active(true)
                    .sortOrder(sort)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            sort += 10;
        }
    }

    private void requireGame(String code) {
        games.findById(code)
                .filter(GameCatalog::isActive)
                .orElseThrow(() -> new BadRequestException("非法或已停用的一级游戏"));
    }

    private void requireGameExists(String code) {
        if (!games.existsById(code)) {
            throw new BadRequestException("一级游戏不存在");
        }
    }

    private TaxonomyCatalogView.GameView gameView(GameCatalog game) {
        List<String> platformCodes = gamePlatforms.findByGameCodeOrderBySortOrderAscIdAsc(game.getCode()).stream()
                .filter(GamePlatform::isActive)
                .map(GamePlatform::getPlatformCode)
                .toList();
        return gameView(game, platformCodes);
    }

    private TaxonomyCatalogView.GameView gameView(GameCatalog game, List<String> platformCodes) {
        return new TaxonomyCatalogView.GameView(
                game.getCode(),
                game.getName(),
                game.getNameEn(),
                game.getAliases(),
                icon(game.getIconKey(), "gamepad"),
                game.getDescription(),
                game.getPublisher(),
                game.getGenres(),
                game.getLogoImage(),
                game.getCoverImage(),
                game.getPopularityScore(),
                game.isFeatured(),
                emptyBusinessStatus(game.getBusinessStatus()),
                game.getSeoSlug(),
                game.getSeoTitle(),
                game.getSeoDescription(),
                game.isActive(),
                game.getSortOrder(),
                categories.countByGameCode(game.getCode()),
                services.countByGameCode(game.getCode()),
                platformCodes);
    }

    private TaxonomyCatalogView.CategoryView categoryView(ServiceCategoryCatalog category) {
        return categoryView(
                category,
                services.countByGameCodeAndCategory(category.getGameCode(), category.getCode()));
    }

    private TaxonomyCatalogView.CategoryView categoryView(
            ServiceCategoryCatalog category, long serviceCount) {
        return new TaxonomyCatalogView.CategoryView(
                category.getId(),
                category.getGameCode(),
                category.getCode(),
                category.getName(),
                icon(category.getIconKey(), "service"),
                category.getDescription(),
                category.isActive(),
                category.getSortOrder(),
                serviceCount);
    }

    private int nextGameSort() {
        return games.findAllByOrderBySortOrderAscCodeAsc().stream()
                .mapToInt(GameCatalog::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private int nextCategorySort(String game) {
        return categories.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(category -> category.getGameCode().equals(game))
                .mapToInt(ServiceCategoryCatalog::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private String uniqueGameCode(String base) {
        String code = base;
        int suffix = 2;
        while (games.existsById(code)) {
            code = clipped(base + "_" + suffix++);
        }
        return code;
    }

    private String uniqueCategoryCode(String game, String base) {
        String code = base;
        int suffix = 2;
        while (categories.findByGameCodeAndCode(game, code).isPresent()) {
            code = clipped(base + "_" + suffix++);
        }
        return code;
    }

    private String clipped(String value) {
        return value.substring(0, Math.min(32, value.length()));
    }

    private String slug(String value) {
        String slug = value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.isBlank()) {
            slug = "ITEM";
        }
        return slug.substring(0, Math.min(28, slug.length()));
    }

    private String normalizeGame(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String icon(String key, String fallback) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        return ICON_KEYS.contains(normalized) ? normalized : fallback;
    }

    private String platformIcon(String group) {
        return switch (group == null ? "" : group.toUpperCase(Locale.ROOT)) {
            case "CONSOLE" -> "console";
            case "MOBILE" -> "mobile";
            default -> "desktop";
        };
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String reason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String businessStatus(String value) {
        String normalized = value == null ? "PLANNED" : value.trim().toUpperCase(Locale.ROOT);
        if (!BUSINESS_STATUSES.contains(normalized)) {
            throw new BadRequestException("非法业务准备状态");
        }
        return normalized;
    }

    private String emptyBusinessStatus(String value) {
        return value == null || value.isBlank() ? "PLANNED" : value;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
