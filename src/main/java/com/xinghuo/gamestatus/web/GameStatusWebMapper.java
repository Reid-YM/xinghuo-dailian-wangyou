package com.xinghuo.gamestatus.web;

import com.xinghuo.gamestatus.application.GameStatusView;
import com.xinghuo.gamestatus.application.UpdateGameStatusCommand;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maps game-status application projections to established HTTP shapes. */
public final class GameStatusWebMapper {
    private GameStatusWebMapper() {
    }

    public static UpdateGameStatusCommand toCommand(
            String gameCode,
            Map<String, Object> body,
            String operator) {
        return new UpdateGameStatusCommand(
                gameCode,
                str(body.get("status")),
                str(body.get("label")),
                str(body.get("detail")),
                str(body.get("ctaLabel")),
                operator);
    }

    public static Map<String, Object> toPublicResponse(GameStatusView view) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("game", view.getGameCode());
        out.put("status", view.getStatusCode().name().toLowerCase(java.util.Locale.ROOT));
        out.put("label", view.getPublicLabel());
        out.put("detail", view.getPublicDetail());
        out.put("ctaLabel", view.getCtaLabel());
        out.put("acceptingOrders", view.getStatusCode().acceptingOrders());
        out.put("effectiveAt", view.getEffectiveAt());
        out.put("updatedAt", view.getUpdatedAt());
        return out;
    }

    public static Map<String, Object> toSummaryResponse(GameStatusView view) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("game", view.getGameCode());
        out.put("status", view.getStatusCode().name().toLowerCase(java.util.Locale.ROOT));
        out.put("label", view.getPublicLabel());
        out.put("ctaLabel", view.getCtaLabel());
        out.put("acceptingOrders", view.getStatusCode().acceptingOrders());
        return out;
    }

    public static Map<String, Object> toAdminResponse(GameStatusView view) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("game", view.getGameCode());
        out.put("status", view.getStatusCode().name().toLowerCase(java.util.Locale.ROOT));
        out.put("label", view.getPublicLabel());
        out.put("updatedAt", view.getUpdatedAt());
        return out;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
