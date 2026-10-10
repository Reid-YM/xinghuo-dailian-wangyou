package com.xinghuo.gamestatus.application;

/** Application-owned command for updating one game's operating status. */
public record UpdateGameStatusCommand(
        String gameCode,
        String statusCode,
        String publicLabel,
        String publicDetail,
        String ctaLabel,
        String operator) {
}
