package com.xinghuo.taxonomy.web;

public record GamePlatformResponse(String gameCode, String platformCode, boolean active, int sortOrder) {
}
