package com.xinghuo.taxonomy.web;

import java.util.List;

public record CatalogResponse(
        List<GameOptionResponse> games,
        List<CategoryOptionResponse> categories,
        List<PlatformOptionResponse> platforms,
        List<GamePlatformResponse> gamePlatforms) {
}
