package com.xinghuo.frontendcontrol.application;

import com.xinghuo.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FrontendControlApplicationService {
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("homeHeroVisible", "true"),
            Map.entry("homeHeroPromisesVisible", "true"),
            Map.entry("homeHeroMediaVisible", "true"),
            Map.entry("homeSecondaryCtaVisible", "true"),
            Map.entry("homeProgressCtaVisible", "true"),
            Map.entry("homeQuotePanelVisible", "true"),
            Map.entry("homeRecordPanelVisible", "true"),
            Map.entry("homeGameEntryVisible", "true"),
            Map.entry("homeWhyVisible", "true"),
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
            Map.entry("themeAccentColor", "#f4bd34"),
            Map.entry("themeBackgroundColor", "#0b0d10"),
            Map.entry("themeSurfaceColor", "#171b21"),
            Map.entry("themeTextColor", "#f5f6f7"),
            Map.entry("themeRadius", "18"),
            Map.entry("themeContentWidth", "1160"),
            Map.entry("themeFontScale", "100")
    );

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "homeHeroVisible", "homeHeroPromisesVisible", "homeHeroMediaVisible",
            "homeSecondaryCtaVisible", "homeProgressCtaVisible", "homeQuotePanelVisible",
            "homeRecordPanelVisible", "homeGameEntryVisible", "homeWhyVisible", "motionEnabled"
    );
    private static final Set<String> URL_KEYS = Set.of(
            "homePrimaryCtaHref", "homeSecondaryCtaHref", "homeProgressCtaHref",
            "homeGameLinkHref", "homeWhyLinkHref"
    );
    private static final Set<String> COLOR_KEYS = Set.of(
            "themeAccentColor", "themeBackgroundColor", "themeSurfaceColor", "themeTextColor"
    );
    private static final Map<String, IntRange> INTEGER_KEYS = Map.of(
            "themeRadius", new IntRange(6, 30),
            "themeContentWidth", new IntRange(960, 1440),
            "themeFontScale", new IntRange(90, 115)
    );

    private final FrontendControlPort port;

    public FrontendControlApplicationService(FrontendControlPort port) {
        this.port = port;
    }

    public Map<String, String> settings() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>(DEFAULTS);
        for (Map.Entry<String, String> entry : port.loadAll().entrySet()) {
            if (!DEFAULTS.containsKey(entry.getKey())) {
                continue;
            }
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                out.put(entry.getKey(), value.trim());
            }
        }
        return out;
    }

    public Map<String, String> update(FrontendControlUpdateCommand command) {
        if (command == null || command.values() == null) {
            throw new BadRequestException("前端配置不能为空");
        }
        for (Map.Entry<String, String> entry : command.values().entrySet()) {
            String key = entry.getKey();
            if (!DEFAULTS.containsKey(key)) {
                throw new BadRequestException("存在不支持的前端配置项：" + key);
            }
            String raw = entry.getValue() == null ? "" : entry.getValue().trim();
            port.put(key, normalize(key, raw));
        }
        return settings();
    }

    private String normalize(String key, String raw) {
        if (BOOLEAN_KEYS.contains(key)) {
            if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                throw new BadRequestException("显示开关必须为 true 或 false：" + key);
            }
            return String.valueOf(Boolean.parseBoolean(raw));
        }
        if (COLOR_KEYS.contains(key)) {
            if (!raw.matches("#[0-9a-fA-F]{6}")) {
                throw new BadRequestException("主题颜色必须使用 #RRGGBB 格式：" + key);
            }
            return raw.toLowerCase();
        }
        IntRange range = INTEGER_KEYS.get(key);
        if (range != null) {
            try {
                int value = Integer.parseInt(raw);
                if (value < range.min() || value > range.max()) {
                    throw new NumberFormatException();
                }
                return String.valueOf(value);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("主题数值超出允许范围：" + key);
            }
        }
        if (URL_KEYS.contains(key)) {
            String value = raw.isBlank() ? DEFAULTS.get(key) : raw;
            if (!isSafeHref(value) || value.length() > 500) {
                throw new BadRequestException("链接仅支持站内路径、页面锚点或 https 地址：" + key);
            }
            return value;
        }
        String value = raw.isBlank() ? DEFAULTS.get(key) : raw;
        int max = "homeHeroKicker".equals(key) ? 240 : (key.endsWith("Title") ? 160 : 120);
        if (value.length() > max) {
            throw new BadRequestException("前端文案内容过长：" + key);
        }
        return value;
    }

    private boolean isSafeHref(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.startsWith("#")) return value.length() > 1;
        if (value.startsWith("/")) return !value.startsWith("//");
        return value.startsWith("https://");
    }

    private record IntRange(int min, int max) {
    }
}
