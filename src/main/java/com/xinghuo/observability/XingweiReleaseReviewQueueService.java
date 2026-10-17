package com.xinghuo.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 把主机发布历史与独立人工确认记录汇总成“待复核队列”。
 *
 * <p>本服务不写生产凭证、不补造历史观察结果，也不允许对已经退役的 release 追认稳定。
 * 历史无确认只能显示 ARCHIVED_UNCONFIRMED；真正可操作的仍只有当前可信生产 release。
 */
@Service
public class XingweiReleaseReviewQueueService {
    private static final String CONFIRMATION_SOURCE = "admin-human-confirmation";
    private static final Pattern HISTORY_FILE = Pattern.compile("^xinghuo-v2-standard-[0-9]{14}\\.json$");
    private static final Pattern RELEASE = Pattern.compile("^xinghuo-v2-standard-[0-9]{14}\\.jar$");
    private static final Pattern GIT_SHA = Pattern.compile("^[0-9a-f]{40}$");
    private static final int LIMIT = 10;
    private static final int RISK_LOOKUP_LIMIT = 4;

    private final ObjectMapper objectMapper;
    private final XingweiReleaseAttestationService attestationService;
    private final XingweiReleaseConfirmationService confirmationService;
    private final XingweiReleaseChangeRiskService changeRiskService;
    private final Path attestationPath;
    private final Path historyDirectory;
    private final Path confirmationDirectory;

    public XingweiReleaseReviewQueueService(
            ObjectMapper objectMapper,
            XingweiReleaseAttestationService attestationService,
            XingweiReleaseConfirmationService confirmationService,
            XingweiReleaseChangeRiskService changeRiskService,
            @Value("${xinghuo.release-attestation.path:/opt/xinghuo-v2/release-attestation.json}") String attestationPath,
            @Value("${xinghuo.release-attestation.history-dir:/opt/xinghuo-v2/release-attestations}") String historyDirectory,
            @Value("${xinghuo.release-confirmation.dir:/opt/xinghuo-v2/release-confirmations}") String confirmationDirectory) {
        this.objectMapper = objectMapper;
        this.attestationService = attestationService;
        this.confirmationService = confirmationService;
        this.changeRiskService = changeRiskService;
        this.attestationPath = Path.of(attestationPath).toAbsolutePath().normalize();
        this.historyDirectory = Path.of(historyDirectory).toAbsolutePath().normalize();
        this.confirmationDirectory = Path.of(confirmationDirectory).toAbsolutePath().normalize();
    }

    public Snapshot snapshot() {
        Instant sampledAt = Instant.now();
        XingweiReleaseChangeRiskService.Rules riskRules = changeRiskService.rules();
        XingweiReleaseAttestationService.Snapshot currentSnapshot = attestationService.snapshot();
        if (!currentSnapshot.available() || currentSnapshot.attestation() == null) {
            return Snapshot.unavailable(sampledAt, currentSnapshot.status(), riskRules);
        }

        String currentRelease = currentSnapshot.attestation().currentRelease();
        Map<String, ReleaseSource> sources = new LinkedHashMap<>();
        ReleaseSource current = readSource(attestationPath);
        if (current != null && currentRelease.equals(current.attestation().currentRelease())) {
            sources.put(currentRelease, current);
        }
        readHistorySources(sources);
        if (!sources.containsKey(currentRelease)) {
            sources.put(currentRelease, new ReleaseSource(currentSnapshot.attestation(), null));
        }

        XingweiReleaseConfirmationService.Snapshot currentConfirmation = confirmationService.snapshot();
        List<ReleaseSource> ordered = sources.values().stream()
                .sorted(Comparator.comparing((ReleaseSource item) -> item.attestation().publishedAt()).reversed())
                .limit(LIMIT)
                .toList();

        long riskStarted = System.nanoTime();
        int riskLookupCount = 0;
        int riskUnknownCount = 0;
        List<ReleaseReview> releases = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            ReleaseSource source = ordered.get(index);
            boolean isCurrent = currentRelease.equals(source.attestation().currentRelease());
            ConfirmationView confirmation = isCurrent
                    ? fromCurrent(currentConfirmation, source.attestation())
                    : readHistoricalConfirmation(source.attestation());
            Integer pr = pullRequestNumber(source.raw());
            XingweiReleaseChangeRiskService.RiskSummary risk;
            if (index < RISK_LOOKUP_LIMIT) {
                riskLookupCount++;
                risk = changeRiskService.riskFor(pr);
            } else {
                risk = new XingweiReleaseChangeRiskService.RiskSummary(
                        false, false, "UNKNOWN", 0, List.of(), XingweiReleaseChangeRiskService.RULES_VERSION, "LOOKUP_LIMIT");
            }
            if ("UNKNOWN".equals(risk.level())) {
                riskUnknownCount++;
            }
            releases.add(toReview(source.attestation(), pr, isCurrent, confirmation, risk));
        }
        long riskElapsedMillis = TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - riskStarted));
        RiskRuntime riskRuntime = new RiskRuntime(
                riskElapsedMillis,
                riskLookupCount,
                riskUnknownCount,
                RISK_LOOKUP_LIMIT,
                riskRules.perPrLookupBudgetMillis(),
                riskLookupCount * riskRules.perPrLookupBudgetMillis());

        long stable = releases.stream().filter(item -> "STABLE".equals(item.confirmationStatus())).count();
        long attention = releases.stream().filter(item -> "NEEDS_ATTENTION".equals(item.confirmationStatus())).count();
        long unconfirmed = releases.stream().filter(item ->
                "PENDING".equals(item.confirmationStatus()) || "UNCONFIRMED".equals(item.confirmationStatus())).count();
        long invalid = releases.stream().filter(item -> "INVALID".equals(item.confirmationStatus())).count();
        long actionableNow = releases.stream()
                .filter(ReleaseReview::current)
                .filter(item -> switch (item.reviewPriority()) {
                    case "REVIEW_NOW", "REVIEW_READY", "FOLLOW_UP" -> true;
                    default -> false;
                })
                .count();
        long archivedGap = releases.stream()
                .filter(item -> "ARCHIVED_GAP".equals(item.reviewPriority()))
                .count();
        List<ReleaseReview> queue = releases.stream()
                .filter(item -> !"COMPLETE".equals(item.actionState()))
                .toList();
        boolean currentNeedsAction = releases.stream()
                .filter(ReleaseReview::current)
                .anyMatch(item -> !"COMPLETE".equals(item.actionState()));
        Summary summary = new Summary(
                releases.size(), stable, attention, unconfirmed, invalid,
                queue.size(), currentNeedsAction, actionableNow, archivedGap);
        return new Snapshot(
                true, "AVAILABLE", sampledAt, summary,
                List.copyOf(queue), List.copyOf(releases), riskRules, riskRuntime);
    }

    private void readHistorySources(Map<String, ReleaseSource> target) {
        if (!Files.isDirectory(historyDirectory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(historyDirectory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(historyDirectory)) {
            stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> HISTORY_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .limit(LIMIT + 4L)
                    .forEach(path -> {
                        ReleaseSource source = readSource(path);
                        if (source != null) {
                            String expected = source.attestation().currentRelease().replace(".jar", ".json");
                            if (expected.equals(path.getFileName().toString())) {
                                target.putIfAbsent(source.attestation().currentRelease(), source);
                            }
                        }
                    });
        } catch (IOException | RuntimeException ignored) {
            // 当前 release 仍可展示；历史目录异常不伪造成“没有历史”。
        }
    }

    private ReleaseSource readSource(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)
                || !Files.isReadable(path)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            XingweiReleaseAttestationService.Attestation attestation = attestationService.parseTrusted(root);
            return attestation == null ? null : new ReleaseSource(attestation, root);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private ConfirmationView fromCurrent(
            XingweiReleaseConfirmationService.Snapshot snapshot,
            XingweiReleaseAttestationService.Attestation release) {
        if (snapshot == null || snapshot.release() == null
                || !release.currentRelease().equals(snapshot.release().currentRelease())
                || !release.gitSha().equals(snapshot.release().gitSha())) {
            return new ConfirmationView(false, "INVALID", false, null, null, null);
        }
        XingweiReleaseConfirmationService.Event latest = snapshot.latest();
        return new ConfirmationView(
                snapshot.available(),
                snapshot.status(),
                snapshot.stableEligible(),
                latest == null ? null : latest.confirmedBy(),
                latest == null ? null : latest.confirmedAt(),
                latest == null ? null : latest.note());
    }

    private ConfirmationView readHistoricalConfirmation(XingweiReleaseAttestationService.Attestation release) {
        if (!RELEASE.matcher(release.currentRelease()).matches() || !GIT_SHA.matcher(release.gitSha()).matches()) {
            return new ConfirmationView(false, "INVALID", false, null, null, null);
        }
        if (!Files.isDirectory(confirmationDirectory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(confirmationDirectory)) {
            return new ConfirmationView(false, "INVALID", false, null, null, null);
        }
        Path file = confirmationDirectory
                .resolve(release.currentRelease().replace(".jar", ".confirmation.json"))
                .normalize();
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return new ConfirmationView(true, "UNCONFIRMED", false, null, null, null);
        }
        if (!file.getParent().equals(confirmationDirectory)
                || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(file)) {
            return new ConfirmationView(false, "INVALID", false, null, null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            if (root.path("schemaVersion").asInt(-1) != 1
                    || !CONFIRMATION_SOURCE.equals(root.path("source").asText(""))
                    || !release.currentRelease().equals(root.path("release").asText(""))
                    || !release.gitSha().equals(root.path("gitSha").asText(""))
                    || !root.path("events").isArray()) {
                return new ConfirmationView(false, "INVALID", false, null, null, null);
            }
            JsonNode latest = null;
            for (JsonNode event : root.path("events")) {
                String decision = event.path("decision").asText("");
                String confirmedBy = event.path("confirmedBy").asText("").trim();
                Instant confirmedAt = parseInstant(event.path("confirmedAt").asText(""));
                String note = event.path("note").asText("");
                if (!("STABLE".equals(decision) || "NEEDS_ATTENTION".equals(decision))
                        || confirmedBy.isEmpty()
                        || confirmedBy.length() > 120
                        || confirmedAt == null
                        || note.length() > 240) {
                    return new ConfirmationView(false, "INVALID", false, null, null, null);
                }
                latest = event;
            }
            if (latest == null) {
                return new ConfirmationView(true, "UNCONFIRMED", false, null, null, null);
            }
            return new ConfirmationView(
                    true,
                    latest.path("decision").asText(),
                    false,
                    latest.path("confirmedBy").asText(),
                    parseInstant(latest.path("confirmedAt").asText()),
                    latest.path("note").asText(""));
        } catch (IOException | RuntimeException ignored) {
            return new ConfirmationView(false, "INVALID", false, null, null, null);
        }
    }

    private ReleaseReview toReview(
            XingweiReleaseAttestationService.Attestation release,
            Integer pullRequestNumber,
            boolean current,
            ConfirmationView confirmation,
            XingweiReleaseChangeRiskService.RiskSummary risk) {
        String actionState;
        if (current) {
            actionState = switch (confirmation.status()) {
                case "STABLE" -> "COMPLETE";
                case "NEEDS_ATTENTION" -> "FOLLOW_UP";
                case "PENDING" -> confirmation.stableEligible() ? "READY" : "WAITING";
                default -> "BLOCKED";
            };
        } else {
            actionState = switch (confirmation.status()) {
                case "STABLE" -> "COMPLETE";
                case "NEEDS_ATTENTION" -> "ARCHIVED_ATTENTION";
                case "UNCONFIRMED" -> "ARCHIVED_UNCONFIRMED";
                default -> "ARCHIVED_INVALID";
            };
        }
        Priority priority = priority(current, actionState, risk);
        return new ReleaseReview(
                current,
                release.publishedAt(),
                release.currentRelease(),
                release.gitSha(),
                pullRequestNumber,
                confirmation.status(),
                confirmation.stableEligible(),
                actionState,
                priority.code(),
                priority.reason(),
                confirmation.confirmedBy(),
                confirmation.confirmedAt(),
                confirmation.note(),
                risk);
    }

    private Priority priority(
            boolean current,
            String actionState,
            XingweiReleaseChangeRiskService.RiskSummary risk) {
        if (!current) {
            return switch (actionState) {
                case "COMPLETE" -> new Priority("ARCHIVED_COMPLETE", "历史版本已有稳定确认，仅保留审计记录");
                case "ARCHIVED_ATTENTION" -> new Priority("ARCHIVED_ATTENTION", "历史版本曾标记继续观察，已退役不可追认");
                default -> new Priority("ARCHIVED_GAP", "历史版本缺少可信稳定结论，已退役不可补写");
            };
        }
        return switch (actionState) {
            case "COMPLETE" -> new Priority("COMPLETE", "当前版本已有人工稳定确认");
            case "FOLLOW_UP" -> new Priority("FOLLOW_UP", "管理员已标记继续观察，需要持续人工复核");
            case "BLOCKED" -> new Priority("BLOCKED", "确认或发布真值不可用，先恢复可信数据再判断");
            case "WAITING" -> new Priority("OBSERVE", "30 分钟自动观察尚未满足稳定确认门槛");
            case "READY" -> {
                if (risk == null || !risk.available() || !risk.complete() || "UNKNOWN".equals(risk.level())) {
                    yield new Priority("REVIEW_NOW", "已具备稳定确认条件，但变更风险信息不完整，优先人工核对");
                }
                if ("HIGH".equals(risk.level())) {
                    yield new Priority("REVIEW_NOW", "已具备稳定确认条件且包含高风险变更类别，优先人工核对");
                }
                yield new Priority("REVIEW_READY", "自动观察完整正常，变更风险信息可用，可按人工流程确认");
            }
            default -> new Priority("BLOCKED", "当前状态无法形成可信复核优先级");
        };
    }

    private Integer pullRequestNumber(JsonNode root) {
        if (root == null) return null;
        JsonNode node = root.path("pullRequestNumber");
        if (!node.isIntegralNumber()) return null;
        int value = node.asInt(-1);
        return value > 0 ? value : null;
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private record ReleaseSource(XingweiReleaseAttestationService.Attestation attestation, JsonNode raw) {}
    private record ConfirmationView(
            boolean available,
            String status,
            boolean stableEligible,
            String confirmedBy,
            Instant confirmedAt,
            String note) {}
    private record Priority(String code, String reason) {}

    public record Snapshot(
            boolean available,
            String status,
            Instant sampledAt,
            Summary summary,
            List<ReleaseReview> queue,
            List<ReleaseReview> releases,
            XingweiReleaseChangeRiskService.Rules riskRules,
            RiskRuntime riskRuntime) {
        static Snapshot unavailable(
                Instant sampledAt,
                String status,
                XingweiReleaseChangeRiskService.Rules rules) {
            return new Snapshot(
                    false, status, sampledAt, Summary.empty(), List.of(), List.of(), rules,
                    RiskRuntime.empty(rules.perPrLookupBudgetMillis()));
        }
    }

    public record Summary(
            int trackedReleaseCount,
            long stableCount,
            long needsAttentionCount,
            long unconfirmedCount,
            long invalidCount,
            int queueCount,
            boolean currentNeedsAction,
            long actionableNowCount,
            long archivedGapCount) {
        static Summary empty() {
            return new Summary(0, 0, 0, 0, 0, 0, false, 0, 0);
        }
    }

    public record RiskRuntime(
            long elapsedMillis,
            int lookupCount,
            int unknownCount,
            int lookupLimit,
            long perLookupBudgetMillis,
            long externalBudgetMillis) {
        static RiskRuntime empty(long perLookupBudgetMillis) {
            return new RiskRuntime(0, 0, 0, RISK_LOOKUP_LIMIT, perLookupBudgetMillis, 0);
        }
    }

    public record ReleaseReview(
            boolean current,
            Instant publishedAt,
            String release,
            String gitSha,
            Integer pullRequestNumber,
            String confirmationStatus,
            boolean stableEligible,
            String actionState,
            String reviewPriority,
            String priorityReason,
            String confirmedBy,
            Instant confirmedAt,
            String note,
            XingweiReleaseChangeRiskService.RiskSummary risk) {}
}
