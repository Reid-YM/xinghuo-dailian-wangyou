package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import com.xinghuo.web.DeviceModeResolver.DeviceMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员可安全调整的前端展示变量。
 *
 * <p>配置分为 global / desktop / mobile 三层。设备层覆盖全局层，但仍使用同一份
 * 白名单和校验规则，不接受任意 CSS / JavaScript。V3.1 增加草稿 / 发布 / 上一版恢复，
 * 公开前台仍只读取正式配置。</p>
 */
@Service
public class FrontendControlService {
    private static final String DEVICE_PREFIX = "frontend.";
    private static final String DRAFT_PREFIX = "frontend.draft.";
    private static final String PREVIOUS_PREFIX = "frontend.previous.";
    private static final String DRAFT_INHERIT_KEY = "__inheritGlobal";

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("siteHeaderVisible", "true"),
            Map.entry("siteFooterVisible", "true"),
            Map.entry("servicesEntryVisible", "true"),
            Map.entry("quoteEntryVisible", "true"),
            Map.entry("recordsEntryVisible", "true"),
            Map.entry("mobileDockVisible", "true"),
            Map.entry("homeHeroVisible", "true"),
            Map.entry("homeHeroPromisesVisible", "true"),
            Map.entry("homeHeroMediaVisible", "true"),
            Map.entry("homeSecondaryCtaVisible", "true"),
            Map.entry("homeProgressCtaVisible", "true"),
            Map.entry("homeQuotePanelVisible", "true"),
            Map.entry("homeRecordPanelVisible", "true"),
            Map.entry("homeGameEntryVisible", "true"),
            Map.entry("homeWhyVisible", "true"),
            Map.entry("homeLauncherVisible", "true"),
            Map.entry("homeHotVisible", "true"),
            Map.entry("homeServicesVisible", "true"),
            Map.entry("homeToolVisible", "true"),
            Map.entry("homeCasesVisible", "true"),
            Map.entry("homeFaqVisible", "true"),
            Map.entry("motionEnabled", "true"),
            Map.entry("homeHeroKicker", "CODMW4 · 可靠手打 · 纯绿服务 · 进度可查"),
            Map.entry("homePrimaryCtaLabel", "提前询价"),
            Map.entry("homePrimaryCtaHref", "#quick-actions"),
            Map.entry("homeSecondaryCtaLabel", "查看服务大厅"),
            Map.entry("homeSecondaryCtaHref", "/services"),
            Map.entry("homeProgressCtaLabel", "查业务进度"),
            Map.entry("homeProgressCtaHref", "/records"),
            Map.entry("homeQuoteEyebrow", "快速询价"),
            Map.entry("homeQuoteTitle", "选择需求，获取报价"),
            Map.entry("homeRecordEyebrow", "业务进度"),
            Map.entry("homeRecordTitle", "查询业务编号"),
            Map.entry("homeGameEyebrow", "游戏入口"),
            Map.entry("homeGameTitle", "选择游戏"),
            Map.entry("homeGameLinkLabel", "全部服务"),
            Map.entry("homeGameLinkHref", "/services"),
            Map.entry("homeWhyEyebrow", "为什么选择星火"),
            Map.entry("homeWhyTitle", "四件可以被验证的事"),
            Map.entry("homeWhyLinkLabel", "更多说明"),
            Map.entry("homeWhyLinkHref", "/trust"),
            Map.entry("homeModuleOrder", ""),
            Map.entry("homeHeroHeight", "0"),
            Map.entry("homeCardScale", "0"),
            Map.entry("mobileDockServicesLabel", "服务"),
            Map.entry("mobileDockGuidesLabel", "攻略"),
            Map.entry("mobileDockRecordsLabel", "记录"),
            Map.entry("mobileDockContactLabel", "客服"),
            Map.entry("mobileDockQuoteLabel", "询价"),
            Map.entry("themeAccentColor", "#f4bd34"),
            Map.entry("themeBackgroundColor", "#0b0d10"),
            Map.entry("themeSurfaceColor", "#171b21"),
            Map.entry("themeTextColor", "#f5f6f7"),
            Map.entry("themeRadius", "18"),
            Map.entry("themeContentWidth", "1160"),
            Map.entry("themeFontScale", "100")
    );

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "siteHeaderVisible", "siteFooterVisible", "servicesEntryVisible", "quoteEntryVisible",
            "recordsEntryVisible", "mobileDockVisible", "homeHeroVisible", "homeHeroPromisesVisible",
            "homeHeroMediaVisible", "homeSecondaryCtaVisible", "homeProgressCtaVisible",
            "homeQuotePanelVisible", "homeRecordPanelVisible", "homeGameEntryVisible",
            "homeWhyVisible", "homeLauncherVisible", "homeHotVisible", "homeServicesVisible",
            "homeToolVisible", "homeCasesVisible", "homeFaqVisible", "motionEnabled"
    );

    private static final Set<String> URL_KEYS = Set.of(
            "homePrimaryCtaHref", "homeSecondaryCtaHref", "homeProgressCtaHref",
            "homeGameLinkHref", "homeWhyLinkHref"
    );

    private static final Set<String> COLOR_KEYS = Set.of(
            "themeAccentColor", "themeBackgroundColor", "themeSurfaceColor", "themeTextColor"
    );

    private static final Map<String, IntRange> INTEGER_KEYS = Map.ofEntries(
            Map.entry("themeRadius", new IntRange(6, 30)),
            Map.entry("themeContentWidth", new IntRange(960, 1440)),
            Map.entry("themeFontScale", new IntRange(90, 115)),
            Map.entry("homeHeroHeight", new IntRange(0, 900)),
            Map.entry("homeCardScale", new IntRange(0, 140))
    );

    private static final Set<String> HOME_MODULE_IDS = Set.of(
            "hero", "launcher", "game", "command", "quote", "support", "why", "hot",
            "services", "tool", "cases", "faq"
    );

    private final SiteSettingRepository repo;

    public FrontendControlService(SiteSettingRepository repo) {
        this.repo = repo;
    }

    public enum Scope {
        GLOBAL("global"),
        DESKTOP("desktop"),
        MOBILE("mobile");

        private final String key;

        Scope(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Scope parse(String raw) {
            String value = raw == null || raw.isBlank() ? "global" : raw.trim().toLowerCase(Locale.ROOT);
            return switch (value) {
                case "global" -> GLOBAL;
                case "desktop" -> DESKTOP;
                case "mobile" -> MOBILE;
                default -> throw new BadRequestException("不支持的前端配置作用域：" + value);
            };
        }
    }

    public record Workspace(
            Map<String, String> published,
            Map<String, String> draft,
            boolean hasDraft,
            boolean hasPrevious,
            boolean draftInheritGlobal
    ) {}

    /** Backward-compatible global settings. */
    @Transactional(readOnly = true)
    public Map<String, String> settings() {
        return settings(Scope.GLOBAL);
    }

    @Transactional(readOnly = true)
    public Map<String, String> settings(String scope) {
        return settings(Scope.parse(scope));
    }

    @Transactional(readOnly = true)
    public Map<String, String> effectiveSettings(DeviceMode mode) {
        return settings(mode == DeviceMode.MOBILE ? Scope.MOBILE : Scope.DESKTOP);
    }

    @Transactional(readOnly = true)
    public Workspace workspace(String scope) {
        Scope parsedScope = Scope.parse(scope);
        List<SiteSetting> all = repo.findAll();
        Map<String, String> published = settings(parsedScope, all);
        Map<String, String> storedDraft = namespaceSnapshot(all, DRAFT_PREFIX, parsedScope);
        boolean inheritGlobal = parsedScope != Scope.GLOBAL && draftInheritanceRequested(all, parsedScope);
        LinkedHashMap<String, String> draft = new LinkedHashMap<>(
                inheritGlobal ? settings(Scope.GLOBAL, all) : published
        );
        if (!inheritGlobal) draft.putAll(storedDraft);
        return new Workspace(
                published,
                draft,
                inheritGlobal || !storedDraft.isEmpty(),
                hasNamespaceEntries(all, PREVIOUS_PREFIX, parsedScope),
                inheritGlobal
        );
    }

    private Map<String, String> settings(Scope scope) {
        return settings(scope, repo.findAll());
    }

    private Map<String, String> settings(Scope scope, List<SiteSetting> all) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>(DEFAULTS);
        for (SiteSetting setting : all) {
            if (!DEFAULTS.containsKey(setting.getSettingKey())) continue;
            String value = setting.getSettingValue();
            if (value != null && !value.isBlank()) {
                out.put(setting.getSettingKey(), value.trim());
            }
        }
        if (scope == Scope.GLOBAL) {
            return out;
        }
        String prefix = scopePrefix(scope);
        for (SiteSetting setting : all) {
            String storageKey = setting.getSettingKey();
            if (storageKey == null || !storageKey.startsWith(prefix)) continue;
            String key = storageKey.substring(prefix.length());
            if (!DEFAULTS.containsKey(key)) continue;
            String value = setting.getSettingValue();
            if (value != null && !value.isBlank()) {
                out.put(key, value.trim());
            }
        }
        return out;
    }

    /** Backward-compatible global update. */
    @Transactional
    public Map<String, String> update(Map<String, Object> values) {
        return update("global", values);
    }

    @Transactional
    public Map<String, String> update(String scope, Map<String, Object> values) {
        Scope parsedScope = Scope.parse(scope);
        Map<String, String> normalized = normalizeValues(values);
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            putInternal(storageKey(parsedScope, entry.getKey()), entry.getValue());
        }
        return settings(parsedScope);
    }

    @Transactional
    public Workspace saveDraft(String scope, Map<String, Object> values) {
        Scope parsedScope = Scope.parse(scope);
        Map<String, String> normalized = normalizeValues(values);
        clearNamespace(DRAFT_PREFIX, parsedScope);
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            putInternal(namespaceKey(DRAFT_PREFIX, parsedScope, entry.getKey()), entry.getValue());
        }
        return workspace(scope);
    }

    @Transactional
    public Workspace stageGlobalInheritance(String scope) {
        Scope parsedScope = Scope.parse(scope);
        if (parsedScope == Scope.GLOBAL) {
            throw new BadRequestException("全局配置不能继承自身");
        }
        clearNamespace(DRAFT_PREFIX, parsedScope);
        putInternal(namespaceKey(DRAFT_PREFIX, parsedScope, DRAFT_INHERIT_KEY), "true");
        return workspace(scope);
    }

    @Transactional
    public Workspace discardDraft(String scope) {
        Scope parsedScope = Scope.parse(scope);
        clearNamespace(DRAFT_PREFIX, parsedScope);
        return workspace(scope);
    }

    @Transactional
    public Workspace publishDraft(String scope) {
        Scope parsedScope = Scope.parse(scope);
        List<SiteSetting> all = repo.findAll();
        Map<String, String> storedDraft = namespaceSnapshot(all, DRAFT_PREFIX, parsedScope);
        boolean inheritGlobal = parsedScope != Scope.GLOBAL && draftInheritanceRequested(all, parsedScope);
        if (storedDraft.isEmpty() && !inheritGlobal) {
            throw new BadRequestException("当前作用域没有可发布的草稿");
        }

        Map<String, String> current = settings(parsedScope, all);
        replaceNamespace(PREVIOUS_PREFIX, parsedScope, current);
        if (inheritGlobal) {
            clearPublishedScope(parsedScope);
        } else {
            LinkedHashMap<String, String> next = new LinkedHashMap<>(current);
            next.putAll(storedDraft);
            replacePublished(parsedScope, next);
        }
        clearNamespace(DRAFT_PREFIX, parsedScope);
        return workspace(scope);
    }

    @Transactional
    public Workspace restorePrevious(String scope) {
        Scope parsedScope = Scope.parse(scope);
        List<SiteSetting> all = repo.findAll();
        Map<String, String> previous = namespaceSnapshot(all, PREVIOUS_PREFIX, parsedScope);
        if (previous.isEmpty()) {
            throw new BadRequestException("当前作用域没有可恢复的上一版配置");
        }

        Map<String, String> current = settings(parsedScope, all);
        replacePublished(parsedScope, previous);
        replaceNamespace(PREVIOUS_PREFIX, parsedScope, current);
        clearNamespace(DRAFT_PREFIX, parsedScope);
        return workspace(scope);
    }

    @Transactional
    public Map<String, String> reset(String scope) {
        Scope parsedScope = Scope.parse(scope);
        if (parsedScope == Scope.GLOBAL) {
            throw new BadRequestException("全局配置不能通过设备覆盖重置接口清空");
        }
        clearPublishedScope(parsedScope);
        return settings(parsedScope);
    }

    private Map<String, String> normalizeValues(Map<String, Object> values) {
        if (values == null) throw new BadRequestException("前端配置不能为空");
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            if ("reason".equals(key)) continue;
            if (!DEFAULTS.containsKey(key)) throw new BadRequestException("存在不支持的前端配置项：" + key);
            String raw = entry.getValue() == null ? "" : String.valueOf(entry.getValue()).trim();
            normalized.put(key, normalize(key, raw));
        }
        return normalized;
    }

    private String normalize(String key, String raw) {
        if (BOOLEAN_KEYS.contains(key)) {
            if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                throw new BadRequestException("显示开关必须为 true 或 false：" + key);
            }
            return String.valueOf(Boolean.parseBoolean(raw));
        }
        if ("homeModuleOrder".equals(key)) {
            return normalizeModuleOrder(raw);
        }
        if (COLOR_KEYS.contains(key)) {
            if (!raw.matches("#[0-9a-fA-F]{6}")) throw new BadRequestException("主题颜色必须使用 #RRGGBB 格式：" + key);
            return raw.toLowerCase(Locale.ROOT);
        }
        IntRange range = INTEGER_KEYS.get(key);
        if (range != null) {
            try {
                int value = Integer.parseInt(raw);
                if (value < range.min() || value > range.max()) throw new NumberFormatException();
                if ("homeCardScale".equals(key) && value != 0 && value < 70) throw new NumberFormatException();
                return String.valueOf(value);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("前端数值超出允许范围：" + key);
            }
        }
        if (URL_KEYS.contains(key)) {
            String value = raw.isBlank() ? DEFAULTS.get(key) : raw;
            if (!isSafeHref(value) || value.length() > 500) throw new BadRequestException("链接仅支持站内路径、页面锚点或 https 地址：" + key);
            return value;
        }
        String value = raw.isBlank() ? DEFAULTS.get(key) : raw;
        int max = "homeHeroKicker".equals(key) ? 240 : (key.endsWith("Title") ? 160 : 120);
        if (value.length() > max) throw new BadRequestException("前端文案内容过长：" + key);
        return value;
    }

    private String normalizeModuleOrder(String raw) {
        if (raw == null || raw.isBlank()) return "";
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String id = token.trim().toLowerCase(Locale.ROOT);
            if (id.isBlank() || !HOME_MODULE_IDS.contains(id)) {
                throw new BadRequestException("首页模块排序包含未知模块：" + token.trim());
            }
            if (!ordered.add(id)) {
                throw new BadRequestException("首页模块排序不能重复：" + id);
            }
        }
        return String.join(",", ordered);
    }

    private boolean isSafeHref(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.startsWith("#")) return value.length() > 1;
        if (value.startsWith("/")) return !value.startsWith("//");
        return value.startsWith("https://");
    }

    private Map<String, String> namespaceSnapshot(List<SiteSetting> all, String namespace, Scope scope) {
        String prefix = namespacePrefix(namespace, scope);
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (SiteSetting setting : all) {
            String storageKey = setting.getSettingKey();
            if (storageKey == null || !storageKey.startsWith(prefix)) continue;
            String key = storageKey.substring(prefix.length());
            if (!DEFAULTS.containsKey(key)) continue;
            String value = setting.getSettingValue();
            if (value != null && !value.isBlank()) values.put(key, value.trim());
        }
        return values;
    }

    private boolean draftInheritanceRequested(List<SiteSetting> all, Scope scope) {
        String key = namespaceKey(DRAFT_PREFIX, scope, DRAFT_INHERIT_KEY);
        return all.stream().anyMatch(setting -> key.equals(setting.getSettingKey())
                && "true".equalsIgnoreCase(setting.getSettingValue()));
    }

    private boolean hasNamespaceEntries(List<SiteSetting> all, String namespace, Scope scope) {
        String prefix = namespacePrefix(namespace, scope);
        return all.stream().map(SiteSetting::getSettingKey)
                .anyMatch(key -> key != null && key.startsWith(prefix));
    }

    private void replacePublished(Scope scope, Map<String, String> values) {
        if (scope != Scope.GLOBAL) clearPublishedScope(scope);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!DEFAULTS.containsKey(entry.getKey())) continue;
            putInternal(storageKey(scope, entry.getKey()), entry.getValue());
        }
    }

    private void replaceNamespace(String namespace, Scope scope, Map<String, String> values) {
        clearNamespace(namespace, scope);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!DEFAULTS.containsKey(entry.getKey())) continue;
            putInternal(namespaceKey(namespace, scope, entry.getKey()), entry.getValue());
        }
    }

    private void clearPublishedScope(Scope scope) {
        if (scope == Scope.GLOBAL) return;
        String prefix = scopePrefix(scope);
        List<String> ids = repo.findAll().stream()
                .map(SiteSetting::getSettingKey)
                .filter(key -> key != null && key.startsWith(prefix))
                .filter(key -> DEFAULTS.containsKey(key.substring(prefix.length())))
                .toList();
        if (!ids.isEmpty()) repo.deleteAllById(ids);
        repo.flush();
    }

    private void clearNamespace(String namespace, Scope scope) {
        String prefix = namespacePrefix(namespace, scope);
        List<String> ids = repo.findAll().stream()
                .map(SiteSetting::getSettingKey)
                .filter(key -> key != null && key.startsWith(prefix))
                .toList();
        if (!ids.isEmpty()) repo.deleteAllById(ids);
        repo.flush();
    }

    private String storageKey(Scope scope, String key) {
        return scope == Scope.GLOBAL ? key : scopePrefix(scope) + key;
    }

    private String scopePrefix(Scope scope) {
        return DEVICE_PREFIX + scope.key() + ".";
    }

    private String namespaceKey(String namespace, Scope scope, String key) {
        return namespacePrefix(namespace, scope) + key;
    }

    private String namespacePrefix(String namespace, Scope scope) {
        return namespace + scope.key() + ".";
    }

    private void putInternal(String key, String value) {
        SiteSetting setting = repo.findById(key).orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        setting.setUpdatedAt(Instant.now());
        repo.save(setting);
    }

    private record IntRange(int min, int max) {}
}
