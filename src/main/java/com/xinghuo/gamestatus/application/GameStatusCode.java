package com.xinghuo.gamestatus.application;

import java.util.Locale;

/** Application-owned game operating status. */
public enum GameStatusCode {
    PREORDER,
    OPEN,
    PAUSED,
    MAINTENANCE,
    LIMITED;

    public boolean acceptingOrders() {
        return this == OPEN;
    }

    public boolean preorder() {
        return this == PREORDER;
    }

    public static GameStatusCode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("状态不能为空");
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "BUSY", "RESERVATION", "PREHEAT" -> PREORDER;
            case "SUSPENDED", "CLOSED" -> PAUSED;
            default -> GameStatusCode.valueOf(value);
        };
    }
}
