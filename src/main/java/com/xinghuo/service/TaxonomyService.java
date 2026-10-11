package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
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
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxonomyService {
    private static final Set<String> ICON_KEYS = Set.of("gamepad", "event", "weapon", "camo", "rank", "service", "quote", "order", "record", "support", "settings", "audit", "desktop", "console", "mobile", "library", "search", "star");
    private static final Set<String> BUSINESS_STATUSES = Set.of("ACTIVE", "READY", "PLANNED", "PAUSED");
    private final GameCatalogRepository games;
    private final ServiceCategoryCatalogRepository categories;
    private final ServiceItemRepository services;
    private final PlatformCatalogRepository platforms;
    private final GamePlatformRepository gamePlatforms;
    private final AuditService audit;
    private volatile Set<String> allowedPairs = null;
    private volatile long allowedPairsLoadedAt = 0L;
    private static final long ALLOWED_PAIRS_TTL_MS = 60000L;

    @Transactional(readOnly=true)
    public ApiDtos.CatalogResponse catalog(boolean admin) {
        List<GamePlatform> allLinks = this.gamePlatforms.findAllByOrderByGameCodeAscSortOrderAscIdAsc();
        HashMap<String, List<String>> platformCodesByGame = new HashMap<>();
        for (GamePlatform gp : allLinks) {
            if (!gp.isActive()) continue;
            platformCodesByGame.computeIfAbsent(gp.getGameCode(), k -> new ArrayList<>()).add(gp.getPlatformCode());
        }
        List<ApiDtos.GameOptionResponse> gs = this.games.findAllByOrderBySortOrderAscCodeAsc().stream().filter(x -> admin || x.isActive()).map(x -> this.gameResponse(x, platformCodesByGame.getOrDefault(x.getCode(), List.of()))).toList();
        HashMap<String, Long> serviceCountByCategory = new HashMap<>();
        for (Object[] row : this.services.countGroupedByGameAndCategory()) {
            serviceCountByCategory.put(String.valueOf(row[0]) + "|" + String.valueOf(row[1]), ((Number)row[2]).longValue());
        }
        List<ApiDtos.CategoryOptionResponse> cs = this.categories.findAllByOrderBySortOrderAscIdAsc().stream().filter(x -> admin || x.isActive() && this.games.findById(x.getGameCode()).map(GameCatalog::isActive).orElse(false) != false).map(x -> new ApiDtos.CategoryOptionResponse(x.getId(), x.getGameCode(), x.getCode(), x.getName(), this.icon(x.getIconKey(), "service"), x.getDescription(), x.isActive(), x.getSortOrder(), serviceCountByCategory.getOrDefault(x.getGameCode() + "|" + x.getCode(), 0L))).toList();
        List<ApiDtos.PlatformOptionResponse> ps = this.platforms.findAllByOrderBySortOrderAscCodeAsc().stream().filter(x -> admin || x.isActive()).map(x -> new ApiDtos.PlatformOptionResponse(x.getCode(), x.getName(), this.icon(x.getIconKey(), this.platformIcon(x.getPlatformGroup())), x.getPlatformGroup(), x.getDescription(), x.isActive(), x.getSortOrder())).toList();
        Set<String> visibleGames = gs.stream().map(ApiDtos.GameOptionResponse::code).collect(Collectors.toSet());
        Set<String> visiblePlatforms = ps.stream().map(ApiDtos.PlatformOptionResponse::code).collect(Collectors.toSet());
        List<ApiDtos.GamePlatformResponse> gps = allLinks.stream().filter(x -> admin || x.isActive() && visibleGames.contains(x.getGameCode()) && visiblePlatforms.contains(x.getPlatformCode())).map(x -> new ApiDtos.GamePlatformResponse(x.getGameCode(), x.getPlatformCode(), x.isActive(), x.getSortOrder())).toList();
        return new ApiDtos.CatalogResponse(gs, cs, ps, gps);
    }

    @Transactional(readOnly=true)
    public GameCatalog publicGameBySeoSlug(String seoSlug) {
        GameCatalog game = (GameCatalog)this.games.findBySeoSlug(seoSlug).orElseThrow(NotFoundException::new);
        if (!game.isActive()) {
            throw new NotFoundException();
        }
        return game;
    }

    @Transactional(readOnly=true)
    public List<GameCatalog> publicGames() {
        return this.games.findAllByOrderBySortOrderAscCodeAsc().stream().filter(GameCatalog::isActive).toList();
    }

    @Transactional
    public ApiDtos.GameOptionResponse addGame(ApiDtos.CatalogNameRequest r, String operator, HttpServletRequest req) {
        String code = this.uniqueGameCode(this.slug(r.name()));
        Instant now = Instant.now();
        boolean active = r.active() == null || r.active() != false;
        GameCatalog x = GameCatalog.builder().code(code).name(r.name().trim()).nameEn(null).aliases(null).iconKey(this.icon(r.iconKey(), "gamepad")).description(this.trim(r.description())).publisher(null).genres(null).logoImage(null).coverImage(null).popularityScore(50).featured(false).businessStatus(active ? "ACTIVE" : "PLANNED").seoSlug(code.toLowerCase(Locale.ROOT).replace('_', '-')).seoTitle(null).seoDescription(null).active(active).sortOrder(r.sortOrder() == null ? this.nextGameSort() : r.sortOrder().intValue()).createdAt(now).updatedAt(now).build();
        this.games.save(x);
        ApiDtos.GameOptionResponse after = this.gameResponse(x);
        this.audit.log("GAME_CREATE", "GAME", code, operator, null, after, this.reason(r.reason(), "\u65b0\u589e\u6e38\u620f"), req);
        return after;
    }

    @Transactional
    public ApiDtos.GameOptionResponse updateGame(String code, ApiDtos.CatalogUpdateRequest r, String operator, HttpServletRequest req) {
        String normalized = this.normalizeGame(code);
        GameCatalog x = (GameCatalog)this.games.findById(normalized).orElseThrow(NotFoundException::new);
        ApiDtos.GameOptionResponse before = this.gameResponse(x);
        x.setName(r.name().trim());
        x.setIconKey(this.icon(r.iconKey(), x.getIconKey() == null ? "gamepad" : x.getIconKey()));
        x.setDescription(this.trim(r.description()));
        if (r.active() != null) {
            x.setActive(r.active().booleanValue());
        }
        if (r.sortOrder() != null) {
            x.setSortOrder(r.sortOrder().intValue());
        }
        x.setUpdatedAt(Instant.now());
        ApiDtos.GameOptionResponse after = this.gameResponse(x);
        this.audit.log("GAME_UPDATE", "GAME", normalized, operator, before, after, this.reason(r.reason(), "\u66f4\u65b0\u6e38\u620f"), req);
        return after;
    }

    @Transactional
    public ApiDtos.GameOptionResponse updateGameProfile(String code, ApiDtos.GameProfileUpdateRequest r, String operator, HttpServletRequest req) {
        String normalized = this.normalizeGame(code);
        GameCatalog x = (GameCatalog)this.games.findById(normalized).orElseThrow(NotFoundException::new);
        ApiDtos.GameOptionResponse before = this.gameResponse(x);
        x.setName(r.name().trim());
        x.setNameEn(this.trim(r.nameEn()));
        x.setAliases(this.trim(r.aliases()));
        x.setIconKey(this.icon(r.iconKey(), x.getIconKey() == null ? "gamepad" : x.getIconKey()));
        x.setDescription(this.trim(r.description()));
        x.setPublisher(this.trim(r.publisher()));
        x.setGenres(this.trim(r.genres()));
        x.setLogoImage(this.trim(r.logoImage()));
        x.setCoverImage(this.trim(r.coverImage()));
        if (r.popularityScore() != null) {
            x.setPopularityScore(r.popularityScore().intValue());
        }
        if (r.featured() != null) {
            x.setFeatured(r.featured().booleanValue());
        }
        if (r.businessStatus() != null) {
            x.setBusinessStatus(this.businessStatus(r.businessStatus()));
        }
        x.setSeoSlug(this.trim(r.seoSlug()));
        x.setSeoTitle(this.trim(r.seoTitle()));
        x.setSeoDescription(this.trim(r.seoDescription()));
        if (r.sortOrder() != null) {
            x.setSortOrder(r.sortOrder().intValue());
        }
        if (r.active() != null) {
            x.setActive(r.active().booleanValue());
        }
        x.setUpdatedAt(Instant.now());
        if (r.platformCodes() != null) {
            this.syncPlatforms(normalized, r.platformCodes());
        }
        ApiDtos.GameOptionResponse after = this.gameResponse(x);
        this.audit.log("GAME_PROFILE_UPDATE", "GAME", normalized, operator, before, after, this.reason(r.reason(), "\u7ef4\u62a4\u6e38\u620f\u8d44\u6599\u4e2d\u5fc3"), req);
        return after;
    }

    @Transactional
    public List<ApiDtos.GameOptionResponse> bulkUpdateGames(ApiDtos.GameBulkUpdateRequest r, String operator, HttpServletRequest req) {
        ArrayList<ApiDtos.GameOptionResponse> out = new ArrayList<ApiDtos.GameOptionResponse>();
        for (String raw : r.codes()) {
            String code = this.normalizeGame(raw);
            GameCatalog x = (GameCatalog)this.games.findById(code).orElseThrow(() -> new BadRequestException("\u6e38\u620f\u4e0d\u5b58\u5728\uff1a" + code));
            ApiDtos.GameOptionResponse before = this.gameResponse(x);
            if (r.active() != null) {
                x.setActive(r.active().booleanValue());
            }
            if (r.featured() != null) {
                x.setFeatured(r.featured().booleanValue());
            }
            if (r.businessStatus() != null) {
                x.setBusinessStatus(this.businessStatus(r.businessStatus()));
            }
            if (r.popularityScore() != null) {
                x.setPopularityScore(r.popularityScore().intValue());
            }
            x.setUpdatedAt(Instant.now());
            ApiDtos.GameOptionResponse after = this.gameResponse(x);
            this.audit.log("GAME_BULK_UPDATE", "GAME", code, operator, before, after, this.reason(r.reason(), "\u6e38\u620f\u8d44\u6599\u6279\u91cf\u8fd0\u8425"), req);
            out.add(after);
        }
        return out;
    }

    @Transactional
    public ApiDtos.CategoryOptionResponse addCategory(ApiDtos.CategoryCreateRequest r, String operator, HttpServletRequest req) {
        String game = this.normalizeGame(r.gameCode());
        this.requireGameExists(game);
        String code = this.uniqueCategoryCode(game, this.slug(r.name()));
        Instant now = Instant.now();
        ServiceCategoryCatalog x = ServiceCategoryCatalog.builder().gameCode(game).code(code).name(r.name().trim()).iconKey(this.icon(r.iconKey(), "service")).description(this.trim(r.description())).active(r.active() == null || r.active() != false).sortOrder(r.sortOrder() == null ? this.nextCategorySort(game) : r.sortOrder().intValue()).createdAt(now).updatedAt(now).build();
        this.categories.save(x);
        this.audit.log("CATEGORY_CREATE", "SERVICE_CATEGORY", game + ":" + code, operator, null, this.categoryResponse(x), this.reason(r.reason(), "\u65b0\u589e\u9700\u6c42\u7c7b\u578b"), req);
        return this.categoryResponse(x);
    }

    @Transactional
    public ApiDtos.CategoryOptionResponse updateCategory(Long id, ApiDtos.CategoryUpdateRequest r, String operator, HttpServletRequest req) {
        ServiceCategoryCatalog x = (ServiceCategoryCatalog)this.categories.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.CategoryOptionResponse before = this.categoryResponse(x);
        x.setName(r.name().trim());
        x.setIconKey(this.icon(r.iconKey(), x.getIconKey() == null ? "service" : x.getIconKey()));
        x.setDescription(this.trim(r.description()));
        if (r.active() != null) {
            x.setActive(r.active().booleanValue());
        }
        if (r.sortOrder() != null) {
            x.setSortOrder(r.sortOrder().intValue());
        }
        x.setUpdatedAt(Instant.now());
        ApiDtos.CategoryOptionResponse after = this.categoryResponse(x);
        this.audit.log("CATEGORY_UPDATE", "SERVICE_CATEGORY", x.getGameCode() + ":" + x.getCode(), operator, before, after, this.reason(r.reason(), "\u66f4\u65b0\u9700\u6c42\u7c7b\u578b"), req);
        return after;
    }

    @Transactional
    public void deleteGame(String code, String operator, HttpServletRequest req) {
        String normalized = this.normalizeGame(code);
        GameCatalog x = (GameCatalog)this.games.findById(normalized).orElseThrow(NotFoundException::new);
        long categoryCount = this.categories.countByGameCode(normalized);
        long serviceCount = this.services.countByGameCode(normalized);
        if (categoryCount > 0L || serviceCount > 0L) {
            throw new ConflictException("\u8be5\u6e38\u620f\u4ecd\u5173\u8054\u9700\u6c42\u7c7b\u578b\u6216\u670d\u52a1\u9879\u76ee\uff0c\u8bf7\u5148\u5904\u7406\u4f9d\u8d56\u540e\u518d\u6c38\u4e45\u5220\u9664");
        }
        ApiDtos.GameOptionResponse before = this.gameResponse(x);
        this.gamePlatforms.deleteByGameCode(normalized);
        this.games.delete(x);
        this.audit.log("GAME_DELETE", "GAME", normalized, operator, before, null, "\u6c38\u4e45\u5220\u9664\u65e0\u4f9d\u8d56\u6e38\u620f", req);
    }

    @Transactional
    public void deleteCategory(Long id, String operator, HttpServletRequest req) {
        ServiceCategoryCatalog x = (ServiceCategoryCatalog)this.categories.findById(id).orElseThrow(NotFoundException::new);
        long serviceCount = this.services.countByGameCodeAndCategory(x.getGameCode(), x.getCode());
        if (serviceCount > 0L) {
            throw new ConflictException("\u8be5\u9700\u6c42\u7c7b\u578b\u4ecd\u5173\u8054\u670d\u52a1\u9879\u76ee\uff0c\u8bf7\u5148\u79fb\u52a8\u6216\u4e0b\u67b6\u5e76\u5904\u7406\u4f9d\u8d56\u540e\u518d\u6c38\u4e45\u5220\u9664");
        }
        ApiDtos.CategoryOptionResponse before = this.categoryResponse(x);
        this.categories.delete(x);
        this.audit.log("CATEGORY_DELETE", "SERVICE_CATEGORY", x.getGameCode() + ":" + x.getCode(), operator, before, null, "\u6c38\u4e45\u5220\u9664\u65e0\u4f9d\u8d56\u9700\u6c42\u7c7b\u578b", req);
    }

    @Transactional(readOnly=true)
    public String normalizeGame(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly=true)
    public void validate(String game, String category) {
        String g = this.normalizeGame(game);
        this.requireGame(g);
        this.categories.findByGameCodeAndCode(g, category == null ? "" : category.trim().toUpperCase(Locale.ROOT)).filter(ServiceCategoryCatalog::isActive).orElseThrow(() -> new BadRequestException("\u975e\u6cd5\u6216\u5df2\u505c\u7528\u7684\u4e8c\u7ea7\u9700\u6c42\u7c7b\u578b"));
    }

    @Transactional(readOnly=true)
    public boolean publicAllowed(String game, String category) {
        String g = this.normalizeGame(game);
        String c = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        return this.allowedPairSet().contains(g + "|" + c);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Set<String> allowedPairSet() {
        Set cached = this.allowedPairs;
        long now = System.currentTimeMillis();
        if (cached != null && now - this.allowedPairsLoadedAt < 60000L) {
            return cached;
        }
        TaxonomyService taxonomyService = this;
        synchronized (taxonomyService) {
            now = System.currentTimeMillis();
            if (this.allowedPairs != null && now - this.allowedPairsLoadedAt < 60000L) {
                return this.allowedPairs;
            }
            Set activeGames = this.games.findAllByOrderBySortOrderAscCodeAsc().stream().filter(GameCatalog::isActive).map(GameCatalog::getCode).collect(Collectors.toSet());
            Set<String> pairs = this.categories.findAllByOrderBySortOrderAscIdAsc().stream().filter(ServiceCategoryCatalog::isActive).filter(x -> activeGames.contains(x.getGameCode())).map(x -> x.getGameCode() + "|" + x.getCode()).collect(Collectors.toSet());
            this.allowedPairs = pairs;
            this.allowedPairsLoadedAt = now;
            return pairs;
        }
    }

    public void invalidateVisibilityCache() {
        this.allowedPairs = null;
        this.allowedPairsLoadedAt = 0L;
    }

    private void syncPlatforms(String gameCode, List<String> rawCodes) {
        List<String> codes = rawCodes.stream().filter(Objects::nonNull).map(x -> x.trim().toUpperCase(Locale.ROOT)).filter(x -> !x.isBlank()).distinct().toList();
        for (String code : codes) {
            if (this.platforms.existsById(code)) continue;
            throw new BadRequestException("\u6e38\u620f\u5e73\u53f0\u4e0d\u5b58\u5728\uff1a" + code);
        }
        this.gamePlatforms.deleteByGameCode(gameCode);
        Instant now = Instant.now();
        int sort = 10;
        for (String code : codes) {
            this.gamePlatforms.save(GamePlatform.builder().gameCode(gameCode).platformCode(code).active(true).sortOrder(sort).createdAt(now).updatedAt(now).build());
            sort += 10;
        }
    }

    private void requireGame(String code) {
        this.games.findById(code).filter(GameCatalog::isActive).orElseThrow(() -> new BadRequestException("\u975e\u6cd5\u6216\u5df2\u505c\u7528\u7684\u4e00\u7ea7\u6e38\u620f"));
    }

    private void requireGameExists(String code) {
        if (!this.games.existsById(code)) {
            throw new BadRequestException("\u4e00\u7ea7\u6e38\u620f\u4e0d\u5b58\u5728");
        }
    }

    private ApiDtos.GameOptionResponse gameResponse(GameCatalog x) {
        List<String> platformCodes = this.gamePlatforms.findByGameCodeOrderBySortOrderAscIdAsc(x.getCode()).stream().filter(GamePlatform::isActive).map(GamePlatform::getPlatformCode).toList();
        return this.gameResponse(x, platformCodes);
    }

    private ApiDtos.GameOptionResponse gameResponse(GameCatalog x, List<String> platformCodes) {
        return new ApiDtos.GameOptionResponse(x.getCode(), x.getName(), x.getNameEn(), x.getAliases(), this.icon(x.getIconKey(), "gamepad"), x.getDescription(), x.getPublisher(), x.getGenres(), x.getLogoImage(), x.getCoverImage(), x.getPopularityScore(), x.isFeatured(), this.emptyBusinessStatus(x.getBusinessStatus()), x.getSeoSlug(), x.getSeoTitle(), x.getSeoDescription(), x.isActive(), x.getSortOrder(), this.categories.countByGameCode(x.getCode()), this.services.countByGameCode(x.getCode()), platformCodes);
    }

    private ApiDtos.CategoryOptionResponse categoryResponse(ServiceCategoryCatalog x) {
        return new ApiDtos.CategoryOptionResponse(x.getId(), x.getGameCode(), x.getCode(), x.getName(), this.icon(x.getIconKey(), "service"), x.getDescription(), x.isActive(), x.getSortOrder(), this.services.countByGameCodeAndCategory(x.getGameCode(), x.getCode()));
    }

    private int nextGameSort() {
        return this.games.findAllByOrderBySortOrderAscCodeAsc().stream().mapToInt(GameCatalog::getSortOrder).max().orElse(0) + 10;
    }

    private int nextCategorySort(String game) {
        return this.categories.findAllByOrderBySortOrderAscIdAsc().stream().filter(x -> x.getGameCode().equals(game)).mapToInt(ServiceCategoryCatalog::getSortOrder).max().orElse(0) + 10;
    }

    private String uniqueGameCode(String base) {
        String c = base;
        int i = 2;
        while (this.games.existsById(c)) {
            c = this.clipped(base + "_" + i++);
        }
        return c;
    }

    private String uniqueCategoryCode(String game, String base) {
        String c = base;
        int i = 2;
        while (this.categories.findByGameCodeAndCode(game, c).isPresent()) {
            c = this.clipped(base + "_" + i++);
        }
        return c;
    }

    private String clipped(String value) {
        return value.substring(0, Math.min(32, value.length()));
    }

    private String slug(String s) {
        String x = s.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (x.isBlank()) {
            x = "ITEM";
        }
        return x.substring(0, Math.min(28, x.length()));
    }

    private String icon(String key, String fallback) {
        String k = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        return ICON_KEYS.contains(k) ? k : fallback;
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
        String x;
        String string = x = value == null ? "PLANNED" : value.trim().toUpperCase(Locale.ROOT);
        if (!BUSINESS_STATUSES.contains(x)) {
            throw new BadRequestException("\u975e\u6cd5\u4e1a\u52a1\u51c6\u5907\u72b6\u6001");
        }
        return x;
    }

    private String emptyBusinessStatus(String value) {
        return value == null || value.isBlank() ? "PLANNED" : value;
    }

    @Generated
    public TaxonomyService(GameCatalogRepository games, ServiceCategoryCatalogRepository categories, ServiceItemRepository services, PlatformCatalogRepository platforms, GamePlatformRepository gamePlatforms, AuditService audit) {
        this.games = games;
        this.categories = categories;
        this.services = services;
        this.platforms = platforms;
        this.gamePlatforms = gamePlatforms;
        this.audit = audit;
    }
}

