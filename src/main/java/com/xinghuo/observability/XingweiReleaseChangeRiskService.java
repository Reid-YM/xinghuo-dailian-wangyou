package com.xinghuo.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 基于公开 GitHub PR 文件列表生成“变更复核优先级”，不参与生产发布成功判定。
 *
 * <p>只向上层返回文件总数、类别和 LOW/MEDIUM/HIGH；不返回具体文件路径。
 * GitHub 不可用、限流、结果不完整或超过查询时延预算时会明确标记 UNKNOWN，绝不降级成低风险。
 */
@Service
public class XingweiReleaseChangeRiskService {
    static final String RULES_VERSION = "v2-20261017";
    static final int MAX_PAGES = 3;
    static final long PER_PR_LOOKUP_BUDGET_MILLIS = 1200L;
    static final long REQUEST_TIMEOUT_CEILING_MILLIS = 700L;
    static final long SUCCESS_CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(6);
    static final long UNKNOWN_CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(90);
    private static final long MIN_REQUEST_TIMEOUT_MILLIS = 100L;
    private static final String REPOSITORY_API =
            "https://api.github.com/repos/Reid-YM/xinghuo-dailian-wangyou";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<Integer, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public XingweiReleaseChangeRiskService(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(PER_PR_LOOKUP_BUDGET_MILLIS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    XingweiReleaseChangeRiskService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public RiskSummary riskFor(Integer pullRequestNumber) {
        if (pullRequestNumber == null || pullRequestNumber <= 0) {
            return RiskSummary.unknown("PR_UNAVAILABLE").withCacheState("NOT_APPLICABLE");
        }
        long now = System.nanoTime();
        CacheEntry cached = cache.get(pullRequestNumber);
        if (cached != null && cached.expiresAtNanos() > now) {
            return cached.summary().withCacheState("HIT");
        }
        if (cached != null) {
            cache.remove(pullRequestNumber, cached);
        }

        RiskSummary resolved = resolve(pullRequestNumber).withCacheState("MISS");
        long ttlMillis = resolved.available() && resolved.complete()
                ? SUCCESS_CACHE_TTL_MILLIS
                : UNKNOWN_CACHE_TTL_MILLIS;
        cache.put(pullRequestNumber, new CacheEntry(
                resolved,
                now + TimeUnit.MILLISECONDS.toNanos(ttlMillis)));
        return resolved;
    }

    private RiskSummary resolve(int pullRequestNumber) {
        List<String> paths = new ArrayList<>();
        boolean complete = false;
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PER_PR_LOOKUP_BUDGET_MILLIS);
        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                long remainingNanos = deadline - System.nanoTime();
                long remainingMillis = TimeUnit.NANOSECONDS.toMillis(Math.max(0L, remainingNanos));
                if (remainingMillis < MIN_REQUEST_TIMEOUT_MILLIS) {
                    return RiskSummary.unknown("LOOKUP_BUDGET_EXHAUSTED");
                }
                long requestTimeoutMillis = Math.min(REQUEST_TIMEOUT_CEILING_MILLIS, remainingMillis);
                URI uri = URI.create(REPOSITORY_API + "/pulls/" + pullRequestNumber
                        + "/files?per_page=100&page=" + page);
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofMillis(requestTimeoutMillis))
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .header("User-Agent", "xinghuo-release-review-queue")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return RiskSummary.unknown("GITHUB_UNAVAILABLE");
                }
                JsonNode root = objectMapper.readTree(response.body());
                if (!root.isArray()) {
                    return RiskSummary.unknown("GITHUB_INVALID_RESPONSE");
                }
                int pageCount = 0;
                for (JsonNode item : root) {
                    String filename = item.path("filename").asText("").trim();
                    if (!filename.isEmpty()) {
                        paths.add(filename);
                        pageCount++;
                    }
                }
                if (pageCount < 100) {
                    complete = true;
                    break;
                }
            }
        } catch (Exception ignored) {
            return RiskSummary.unknown(
                    System.nanoTime() >= deadline ? "LOOKUP_BUDGET_EXHAUSTED" : "GITHUB_UNAVAILABLE");
        }

        RiskSummary classified = classifyPaths(paths);
        if (!complete) {
            return new RiskSummary(
                    true,
                    false,
                    "UNKNOWN",
                    classified.changedFileCount(),
                    classified.categories(),
                    RULES_VERSION,
                    "PR_FILE_LIST_TRUNCATED",
                    "DIRECT");
        }
        return classified;
    }

    RiskSummary classifyPaths(List<String> paths) {
        if (paths == null) {
            return RiskSummary.unknown("NO_FILE_LIST");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : paths) {
            String path = raw == null ? "" : raw.trim();
            if (!path.isEmpty()) {
                unique.add(path);
            }
        }

        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (String path : unique) {
            categories.add(category(path));
        }
        categories.remove("OTHER");
        if (categories.isEmpty() && !unique.isEmpty()) {
            categories.add("OTHER");
        }

        String level = riskLevel(categories);
        return new RiskSummary(
                true,
                true,
                level,
                unique.size(),
                List.copyOf(categories),
                RULES_VERSION,
                "GITHUB_PR_FILES",
                "DIRECT");
    }

    private String category(String path) {
        if (path.startsWith("src/test/") || path.contains("/test/")) return "TESTS";
        if (path.startsWith("docs/") || path.endsWith(".md")) return "DOCS";
        if (path.startsWith("src/main/resources/db/migration/")) return "DATABASE";
        if (path.startsWith("src/main/java/com/xinghuo/payment/")) return "PAYMENT";
        if (path.startsWith("src/main/java/com/xinghuo/security/")) return "SECURITY";
        if (path.startsWith(".github/workflows/deploy-")
                || path.startsWith("scripts/deploy-")
                || path.startsWith("scripts/github-deploy")
                || path.startsWith("scripts/install-github-deploy")) return "DEPLOYMENT";
        if (path.startsWith("src/main/java/com/xinghuo/wecom/")) return "WECOM";
        if (path.startsWith("src/main/java/com/xinghuo/account/")) return "ACCOUNT";
        if (path.startsWith("src/main/java/com/xinghuo/observability/")) return "OBSERVABILITY";
        if (path.startsWith("src/main/resources/static/next/")
                || path.contains("FrontendNext")) return "FRONTEND_NEXT";
        if ((path.startsWith("src/main/resources/static/assets/admin-")
                || path.startsWith("src/main/resources/templates/admin-"))) return "ADMIN_UI";
        if (path.startsWith("src/main/java/")) return "BACKEND";
        if (path.startsWith("src/main/resources/templates/")
                || path.startsWith("src/main/resources/static/")) return "LEGACY_FRONTEND";
        if (path.startsWith(".github/workflows/") || path.startsWith("scripts/")) return "TOOLING";
        return "OTHER";
    }

    private String riskLevel(Set<String> categories) {
        if (categories.contains("DATABASE")
                || categories.contains("PAYMENT")
                || categories.contains("SECURITY")
                || categories.contains("DEPLOYMENT")) {
            return "HIGH";
        }
        if (categories.stream().anyMatch(category -> switch (category) {
            case "WECOM", "ACCOUNT", "OBSERVABILITY", "ADMIN_UI", "BACKEND", "LEGACY_FRONTEND", "TOOLING", "OTHER" -> true;
            default -> false;
        })) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public Rules rules() {
        return new Rules(
                RULES_VERSION,
                List.of("DATABASE", "PAYMENT", "SECURITY", "DEPLOYMENT"),
                List.of("WECOM", "ACCOUNT", "OBSERVABILITY", "ADMIN_UI", "BACKEND", "LEGACY_FRONTEND", "TOOLING", "OTHER"),
                "其余仅 TESTS / DOCS / FRONTEND_NEXT 时为 LOW；数据缺失、超出预算或列表不完整时为 UNKNOWN",
                PER_PR_LOOKUP_BUDGET_MILLIS,
                SUCCESS_CACHE_TTL_MILLIS,
                UNKNOWN_CACHE_TTL_MILLIS);
    }

    private record CacheEntry(RiskSummary summary, long expiresAtNanos) {}

    public record RiskSummary(
            boolean available,
            boolean complete,
            String level,
            int changedFileCount,
            List<String> categories,
            String rulesVersion,
            String source,
            String cacheState) {
        public RiskSummary(
                boolean available,
                boolean complete,
                String level,
                int changedFileCount,
                List<String> categories,
                String rulesVersion,
                String source) {
            this(available, complete, level, changedFileCount, categories, rulesVersion, source, "DIRECT");
        }

        static RiskSummary unknown(String source) {
            return new RiskSummary(false, false, "UNKNOWN", 0, List.of(), RULES_VERSION, source, "DIRECT");
        }

        RiskSummary withCacheState(String cacheState) {
            return new RiskSummary(
                    available, complete, level, changedFileCount, categories, rulesVersion, source, cacheState);
        }
    }

    public record Rules(
            String version,
            List<String> high,
            List<String> medium,
            String lowOrUnknown,
            long perPrLookupBudgetMillis,
            long successCacheTtlMillis,
            long unknownCacheTtlMillis) {
        public Rules(String version, List<String> high, List<String> medium, String lowOrUnknown) {
            this(version, high, medium, lowOrUnknown,
                    PER_PR_LOOKUP_BUDGET_MILLIS,
                    SUCCESS_CACHE_TTL_MILLIS,
                    UNKNOWN_CACHE_TTL_MILLIS);
        }
    }
}
