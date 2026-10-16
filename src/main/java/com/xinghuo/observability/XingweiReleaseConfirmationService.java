package com.xinghuo.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 保存“人工确认”而不是“生产发布真值”。
 *
 * <p>生产是否成功仍只由 {@link XingweiReleaseAttestationService} 读取主机网关凭证判定。
 * 本服务把管理员对当前 release 的人工结论写入独立目录，并在每次写入前后重新核对
 * current release / Git SHA，防止发布切换期间把旧页面的确认误写到新版本。
 */
@Service
public class XingweiReleaseConfirmationService {
    private static final String SOURCE = "admin-human-confirmation";
    private static final Pattern GIT_SHA = Pattern.compile("^[0-9a-f]{40}$");
    private static final Pattern RELEASE = Pattern.compile("^xinghuo-v2-standard-[0-9]{14}\\.jar$");
    private static final int MAX_NOTE_LENGTH = 240;

    private final ObjectMapper objectMapper;
    private final XingweiReleaseAttestationService attestationService;
    private final XingweiReleaseStabilityService stabilityService;
    private final Path attestationPath;
    private final Path confirmationDirectory;

    public XingweiReleaseConfirmationService(
            ObjectMapper objectMapper,
            XingweiReleaseAttestationService attestationService,
            XingweiReleaseStabilityService stabilityService,
            @Value("${xinghuo.release-attestation.path:/opt/xinghuo-v2/release-attestation.json}") String attestationPath,
            @Value("${xinghuo.release-confirmation.dir:/opt/xinghuo-v2/release-confirmations}") String confirmationDirectory) {
        this.objectMapper = objectMapper;
        this.attestationService = attestationService;
        this.stabilityService = stabilityService;
        this.attestationPath = Path.of(attestationPath).toAbsolutePath().normalize();
        this.confirmationDirectory = Path.of(confirmationDirectory).toAbsolutePath().normalize();
    }

    public Snapshot snapshot() {
        XingweiReleaseAttestationService.Snapshot release = attestationService.snapshot();
        XingweiReleaseStabilityService.Snapshot stability = stabilityService.snapshot();
        return snapshot(release, stability);
    }

    public synchronized Snapshot confirm(Command command, String actor) {
        if (command == null) {
            throw new Rejected("确认参数不能为空");
        }
        String decision = normalizeDecision(command.decision());
        String note = normalizeNote(command.note());
        String expectedRelease = normalize(command.expectedRelease());
        String expectedGitSha = normalize(command.expectedGitSha());
        String confirmedBy = normalize(actor);
        if (!RELEASE.matcher(expectedRelease).matches() || !GIT_SHA.matcher(expectedGitSha).matches()) {
            throw new Rejected("页面发布标识无效，请刷新后重试");
        }
        if (confirmedBy.isEmpty() || confirmedBy.length() > 120) {
            throw new Rejected("当前管理员身份不可用于确认");
        }
        if ("NEEDS_ATTENTION".equals(decision) && note.isEmpty()) {
            throw new Rejected("标记需要继续观察时必须填写说明");
        }

        XingweiReleaseAttestationService.Snapshot before = attestationService.snapshot();
        XingweiReleaseAttestationService.Attestation attestation = requireExpected(before, expectedRelease, expectedGitSha);
        XingweiReleaseStabilityService.Snapshot stability = stabilityService.snapshot();
        Eligibility eligibility = eligibility(stability);
        if ("STABLE".equals(decision) && !eligibility.eligible()) {
            throw new Rejected("30 分钟观察尚未完整正常，不能确认稳定");
        }

        XingweiReleaseAttestationService.Snapshot after = attestationService.snapshot();
        requireExpected(after, expectedRelease, expectedGitSha);

        Path file = confirmationFile(attestation.currentRelease());
        List<Event> history = readHistory(file, attestation.currentRelease(), attestation.gitSha());
        if (history == null) {
            throw new StorageUnavailable("已有人工确认记录无法安全读取");
        }

        XingweiReleaseStabilityService.CurrentObservation current = stability.current();
        XingweiReleaseStabilityService.WindowObservation window30 = window30(current);
        Event event = new Event(
                decision,
                note,
                confirmedBy,
                Instant.now(),
                current == null ? "UNKNOWN" : current.severity(),
                current == null ? "UNAVAILABLE" : current.coverageState(),
                window30 == null ? "UNAVAILABLE" : window30.state(),
                window30 == null ? 0L : window30.serverErrors(),
                window30 == null ? 0L : window30.totalRequests());
        List<Event> nextHistory = new ArrayList<>(history);
        nextHistory.add(event);
        write(file, attestation, releaseReference(attestation), nextHistory);

        XingweiReleaseAttestationService.Snapshot finalRelease = attestationService.snapshot();
        requireExpected(finalRelease, expectedRelease, expectedGitSha);
        return snapshot(finalRelease, stabilityService.snapshot());
    }

    private Snapshot snapshot(
            XingweiReleaseAttestationService.Snapshot release,
            XingweiReleaseStabilityService.Snapshot stability) {
        Instant sampledAt = Instant.now();
        if (!release.available() || release.attestation() == null) {
            return Snapshot.unavailable(sampledAt, release.status());
        }
        XingweiReleaseAttestationService.Attestation attestation = release.attestation();
        ReleaseReference reference = releaseReference(attestation);
        Eligibility eligibility = eligibility(stability);
        if (!trustedDirectory()) {
            return new Snapshot(true, "STORAGE_MISSING", sampledAt, reference, eligibility.eligible(), eligibility.reason(), null, List.of());
        }
        List<Event> history = readHistory(confirmationFile(attestation.currentRelease()), attestation.currentRelease(), attestation.gitSha());
        if (history == null) {
            return new Snapshot(false, "INVALID", sampledAt, reference, eligibility.eligible(), eligibility.reason(), null, List.of());
        }
        Event latest = history.isEmpty() ? null : history.get(history.size() - 1);
        String status = latest == null ? "PENDING" : latest.decision();
        return new Snapshot(true, status, sampledAt, reference, eligibility.eligible(), eligibility.reason(), latest, List.copyOf(history));
    }

    private XingweiReleaseAttestationService.Attestation requireExpected(
            XingweiReleaseAttestationService.Snapshot snapshot,
            String expectedRelease,
            String expectedGitSha) {
        if (!snapshot.available() || snapshot.attestation() == null) {
            throw new Conflict("当前生产发布凭证不可用，请刷新后重试");
        }
        XingweiReleaseAttestationService.Attestation attestation = snapshot.attestation();
        if (!expectedRelease.equals(attestation.currentRelease()) || !expectedGitSha.equals(attestation.gitSha())) {
            throw new Conflict("生产版本已变化，请刷新页面后重新确认");
        }
        return attestation;
    }

    private ReleaseReference releaseReference(XingweiReleaseAttestationService.Attestation attestation) {
        return new ReleaseReference(
                attestation.currentRelease(),
                attestation.gitSha(),
                readPullRequestNumber(attestation),
                attestation.publishedAt());
    }

    private Integer readPullRequestNumber(XingweiReleaseAttestationService.Attestation attestation) {
        if (!Files.isRegularFile(attestationPath, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(attestationPath)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(attestationPath.toFile());
            if (!attestation.gitSha().equals(root.path("gitSha").asText(""))
                    || !attestation.currentRelease().equals(root.path("currentRelease").asText(""))) {
                return null;
            }
            JsonNode node = root.path("pullRequestNumber");
            if (!node.isIntegralNumber()) {
                return null;
            }
            int value = node.asInt(-1);
            return value > 0 ? value : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private Eligibility eligibility(XingweiReleaseStabilityService.Snapshot stability) {
        if (stability == null || stability.current() == null || !stability.current().available()) {
            return new Eligibility(false, "OBSERVATION_UNAVAILABLE");
        }
        XingweiReleaseStabilityService.CurrentObservation current = stability.current();
        if (!"COVERED".equals(current.coverageState())) {
            return new Eligibility(false, "COVERAGE_INCOMPLETE");
        }
        XingweiReleaseStabilityService.WindowObservation window30 = window30(current);
        if (window30 == null || !"COMPLETE".equals(window30.state())) {
            return new Eligibility(false, "WINDOW_IN_PROGRESS");
        }
        if (!"OK".equals(current.severity()) || !"OK".equals(window30.severity())) {
            return new Eligibility(false, "AUTOMATED_ALERT");
        }
        return new Eligibility(true, "ELIGIBLE");
    }

    private XingweiReleaseStabilityService.WindowObservation window30(
            XingweiReleaseStabilityService.CurrentObservation current) {
        if (current == null || current.windows() == null) {
            return null;
        }
        return current.windows().stream()
                .filter(item -> item.minutes() == 30)
                .findFirst()
                .orElse(null);
    }

    private Path confirmationFile(String release) {
        if (!RELEASE.matcher(release).matches()) {
            throw new StorageUnavailable("release 文件名无效");
        }
        return confirmationDirectory.resolve(release.replace(".jar", ".confirmation.json")).normalize();
    }

    private boolean trustedDirectory() {
        return Files.isDirectory(confirmationDirectory, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(confirmationDirectory)
                && Files.isReadable(confirmationDirectory);
    }

    private List<Event> readHistory(Path file, String release, String gitSha) {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        if (!trustedDirectory()
                || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(file)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            if (root.path("schemaVersion").asInt(-1) != 1
                    || !SOURCE.equals(root.path("source").asText(""))
                    || !release.equals(root.path("release").asText(""))
                    || !gitSha.equals(root.path("gitSha").asText(""))) {
                return null;
            }
            JsonNode events = root.path("events");
            if (!events.isArray()) {
                return null;
            }
            List<Event> parsed = new ArrayList<>();
            for (JsonNode node : events) {
                Event event = parseEvent(node);
                if (event == null) {
                    return null;
                }
                parsed.add(event);
            }
            return parsed;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private Event parseEvent(JsonNode node) {
        String decision;
        try {
            decision = normalizeDecision(node.path("decision").asText(""));
        } catch (Rejected ignored) {
            return null;
        }
        String note = normalizeNote(node.path("note").asText(""));
        String confirmedBy = normalize(node.path("confirmedBy").asText(""));
        Instant confirmedAt = parseInstant(node.path("confirmedAt").asText(""));
        String severity = normalize(node.path("observationSeverity").asText("UNKNOWN"));
        String coverage = normalize(node.path("coverageState").asText("UNAVAILABLE"));
        String windowState = normalize(node.path("window30State").asText("UNAVAILABLE"));
        long serverErrors = Math.max(0L, node.path("window30ServerErrors").asLong(0L));
        long totalRequests = Math.max(0L, node.path("window30TotalRequests").asLong(0L));
        if (confirmedBy.isEmpty() || confirmedBy.length() > 120 || confirmedAt == null || note.length() > MAX_NOTE_LENGTH) {
            return null;
        }
        return new Event(decision, note, confirmedBy, confirmedAt, severity, coverage, windowState, serverErrors, totalRequests);
    }

    private void write(
            Path file,
            XingweiReleaseAttestationService.Attestation attestation,
            ReleaseReference reference,
            List<Event> history) {
        if (!trustedDirectory() || !Files.isWritable(confirmationDirectory)) {
            throw new StorageUnavailable("人工确认目录不可写");
        }
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(file)) {
            throw new StorageUnavailable("人工确认目标不是普通文件");
        }
        Path temp = confirmationDirectory.resolve(file.getFileName() + ".next-" + UUID.randomUUID()).normalize();
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schemaVersion", 1);
            root.put("source", SOURCE);
            root.put("release", attestation.currentRelease());
            root.put("gitSha", attestation.gitSha());
            if (reference.pullRequestNumber() != null) {
                root.put("pullRequestNumber", reference.pullRequestNumber());
            }
            ArrayNode events = root.putArray("events");
            for (Event item : history) {
                ObjectNode node = events.addObject();
                node.put("decision", item.decision());
                node.put("note", item.note());
                node.put("confirmedBy", item.confirmedBy());
                node.put("confirmedAt", item.confirmedAt().toString());
                node.put("observationSeverity", item.observationSeverity());
                node.put("coverageState", item.coverageState());
                node.put("window30State", item.window30State());
                node.put("window30ServerErrors", item.window30ServerErrors());
                node.put("window30TotalRequests", item.window30TotalRequests());
            }
            Files.writeString(
                    temp,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            try {
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-r-----"));
            } catch (UnsupportedOperationException ignored) {
                // 非 POSIX 测试环境不降低功能正确性；生产 Linux 会应用 0640。
            }
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                throw new StorageUnavailable("文件系统不支持原子人工确认写入", ex);
            }
        } catch (IOException ex) {
            throw new StorageUnavailable("人工确认写入失败", ex);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // 临时文件清理失败不覆盖主异常。
            }
        }
    }

    private String normalizeDecision(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (!"STABLE".equals(normalized) && !"NEEDS_ATTENTION".equals(normalized)) {
            throw new Rejected("确认结论仅允许 STABLE 或 NEEDS_ATTENTION");
        }
        return normalized;
    }

    private String normalizeNote(String value) {
        String normalized = normalize(value).replace('\r', ' ').replace('\n', ' ');
        if (normalized.length() > MAX_NOTE_LENGTH) {
            throw new Rejected("确认说明不能超过 240 字符");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public record Command(String decision, String note, String expectedRelease, String expectedGitSha) {}

    public record Snapshot(
            boolean available,
            String status,
            Instant sampledAt,
            ReleaseReference release,
            boolean stableEligible,
            String stableEligibility,
            Event latest,
            List<Event> history) {
        static Snapshot unavailable(Instant sampledAt, String status) {
            return new Snapshot(false, status, sampledAt, null, false, "OBSERVATION_UNAVAILABLE", null, List.of());
        }
    }

    public record ReleaseReference(
            String currentRelease,
            String gitSha,
            Integer pullRequestNumber,
            Instant publishedAt) {}

    public record Event(
            String decision,
            String note,
            String confirmedBy,
            Instant confirmedAt,
            String observationSeverity,
            String coverageState,
            String window30State,
            long window30ServerErrors,
            long window30TotalRequests) {}

    private record Eligibility(boolean eligible, String reason) {}

    public static class Conflict extends RuntimeException {
        public Conflict(String message) { super(message); }
    }

    public static class Rejected extends RuntimeException {
        public Rejected(String message) { super(message); }
    }

    public static class StorageUnavailable extends RuntimeException {
        public StorageUnavailable(String message) { super(message); }
        public StorageUnavailable(String message, Throwable cause) { super(message, cause); }
    }
}
