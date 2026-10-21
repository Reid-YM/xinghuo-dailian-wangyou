package com.xinghuo.web;

import com.xinghuo.guide.GuideModels;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.HtmlUtils;

/**
 * Renders public data already selected by the canonical page controller.
 * No queries, external requests, private account fields, or write operations.
 * Content is embedded inside <noscript>, preserving the interactive Next runtime.
 */
final class PublicCatalogFallback {
    private PublicCatalogFallback() {}

    static String markup(String path, Map<?, ?> model) {
        if (model == null || model.isEmpty()) return "";
        if ("/".equals(path)) return serviceList(model.get("homeServices"), "当前可查看的服务", 6);
        if ("/services".equals(path)) return serviceList(model.get("hallServices"), "公开服务目录", 24);
        if ("/guides".equals(path)) return guideList(model.get("guides"), "已发布攻略", 30);
        if (path.startsWith("/service/")) return serviceDetail(path, model);
        if (path.startsWith("/guides/")) return guideDetail(path, model);
        if (path.startsWith("/game/")) return gameDetail(path, model);
        return "";
    }

    private static String serviceList(Object rows, String heading, int limit) {
        if (!(rows instanceof List<?> list)) return "";
        StringBuilder items = new StringBuilder();
        int count = 0;
        for (Object value : list) {
            if (!(value instanceof ServiceItem service) || !publicService(service)) continue;
            String slug = safeSlug(service.getSlug());
            if (slug == null) continue;
            items.append("<li><a href=\"/service/").append(slug).append("\">")
                 .append(escape(service.getName())).append("</a>");
            if (service.getShortDesc() != null && !service.getShortDesc().isBlank()) {
                items.append(" — ").append(escape(shortText(service.getShortDesc(), 240)));
            }
            items.append("</li>");
            if (++count >= limit) break;
        }
        return section(heading, items);
    }

    private static String guideList(Object rows, String heading, int limit) {
        if (!(rows instanceof List<?> list)) return "";
        StringBuilder items = new StringBuilder();
        int count = 0;
        for (Object value : list) {
            if (!(value instanceof GuideModels.GuideSummary guide) || !publicGuide(guide)) continue;
            String slug = safeSlug(guide.slug());
            if (slug == null) continue;
            items.append("<li><a href=\"/guides/").append(slug).append("\">")
                 .append(escape(guide.title())).append("</a>");
            if (guide.summary() != null && !guide.summary().isBlank()) {
                items.append(" — ").append(escape(shortText(guide.summary(), 240)));
            }
            items.append("</li>");
            if (++count >= limit) break;
        }
        return section(heading, items);
    }

    private static String serviceDetail(String path, Map<?, ?> model) {
        if (!(model.get("service") instanceof ServiceItem service) || !publicService(service)) return "";
        String slug = safeSlug(service.getSlug());
        if (slug == null || !path.equals("/service/" + slug)) return "";
        StringBuilder html = new StringBuilder("<div><h2>")
                .append(escape(service.getName())).append("</h2>");
        if (service.getShortDesc() != null) html.append("<p>")
                .append(escape(shortText(service.getShortDesc(), 360))).append("</p>");
        if (service.getDurationText() != null && !service.getDurationText().isBlank()) {
            html.append("<p>参考耗时：").append(escape(service.getDurationText())).append("</p>");
        }
        html.append("<p><a href=\"/quote?service=").append(slug)
                .append("\">咨询该服务的实际报价</a></p>");
        html.append(serviceList(model.get("relatedServices"), "相关服务", 4));
        return html.append("</div>").toString();
    }

    private static String guideDetail(String path, Map<?, ?> model) {
        if (!(model.get("guide") instanceof GuideModels.GuideSummary guide) || !publicGuide(guide)) return "";
        String slug = safeSlug(guide.slug());
        if (slug == null || !path.equals("/guides/" + slug)) return "";
        StringBuilder html = new StringBuilder("<article><h2>")
                .append(escape(guide.title())).append("</h2>");
        if (guide.summary() != null && !guide.summary().isBlank()) {
            html.append("<p>").append(escape(shortText(guide.summary(), 420))).append("</p>");
        }
        Object sections = model.get("sections");
        if (sections instanceof List<?> list) {
            int count = 0;
            for (Object value : list) {
                if (!(value instanceof GuideModels.GuideSection section)) continue;
                if (section.heading() != null && !section.heading().isBlank()) {
                    html.append("<h3>").append(escape(section.heading())).append("</h3>");
                }
                if (section.bodyMarkdown() != null && !section.bodyMarkdown().isBlank()) {
                    html.append("<p>").append(escape(shortText(section.bodyMarkdown(), 600))).append("</p>");
                }
                if (++count >= 5) break;
            }
        }
        html.append("<p><a href=\"/guides\">返回攻略中心</a></p></article>");
        return html.toString();
    }

    private static String gameDetail(String path, Map<?, ?> model) {
        if (!(model.get("game") instanceof GameCatalog game) || !game.isActive()) return "";
        String slug = safeSlug(game.getSeoSlug());
        if (slug == null || !path.equals("/game/" + slug)) return "";
        StringBuilder html = new StringBuilder("<section><h2>")
                .append(escape(game.getName())).append("</h2>");
        if (game.getDescription() != null && !game.getDescription().isBlank()) {
            html.append("<p>").append(escape(shortText(game.getDescription(), 320))).append("</p>");
        }
        html.append(serviceList(model.get("gameServices"), "该游戏相关服务", 12));
        return html.append("</section>").toString();
    }

    private static boolean publicService(ServiceItem service) {
        return service.isActive()
                && service.getStatus() != null
                && service.getStatus() != ServiceItem.Status.OFFLINE
                && (service.getExpiresAt() == null || service.getExpiresAt().isAfter(Instant.now()));
    }

    private static boolean publicGuide(GuideModels.GuideSummary guide) {
        return "PUBLISHED".equals(guide.status())
                && guide.publishedAt() != null
                && !guide.publishedAt().isAfter(Instant.now());
    }

    private static String section(String heading, StringBuilder items) {
        if (items.length() == 0) return "";
        return "<section aria-label=\"" + escape(heading) + "\"><h2>" + escape(heading)
                + "</h2><ul>" + items + "</ul></section>";
    }

    private static String safeSlug(String value) {
        return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,119}") ? value : null;
    }

    private static String shortText(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
