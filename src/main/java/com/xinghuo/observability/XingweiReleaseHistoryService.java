package com.xinghuo.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 读取生产发布网关保存的历史发布凭证。
 *
 * <p>历史文件只由生产发布脚本写入，应用只读并复用与当前发布凭证完全相同的可信解析规则。
 * 对外只返回 release 文件名、哈希和聚合健康对比，不返回服务器目录或解析异常细节。
 */
@Service
public class XingweiReleaseHistoryService {
    static final int HISTORY_LIMIT = 20;
    private static final Pattern HISTORY_FILE = Pattern.compile("^xinghuo-v2-standard-[0-9]{14}\\.json$");

    private final ObjectMapper objectMapper;
    private final XingweiReleaseAttestationService attestationService;
    private final Path historyDirectory;

    public XingweiReleaseHistoryService(
            ObjectMapper objectMapper,
            XingweiReleaseAttestationService attestationService,
            @Value("${xinghuo.release-attestation.history-dir:/opt/xinghuo-v2/release-attestations}") String historyDirectory) {
        this.objectMapper = objectMapper;
        this.attestationService = attestationService;
        this.historyDirectory = Path.of(historyDirectory).toAbsolutePath().normalize();
    }

    public Snapshot snapshot() {
        Instant sampledAt = Instant.now();
        if (!Files.isDirectory(historyDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return Snapshot.unavailable(sampledAt, "MISSING");
        }

        try (Stream<Path> stream = Files.list(historyDirectory)) {
            List<Path> candidates = stream
                    .filter(this::isTrustedHistoryFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .limit(HISTORY_LIMIT)
                    .toList();

            List<Entry> entries = new ArrayList<>(candidates.size());
            for (Path path : candidates) {
                Entry entry = readEntry(path);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            entries.sort(Comparator.comparing(Entry::publishedAt).reversed());
            if (entries.isEmpty()) {
                return new Snapshot(true, "EMPTY", sampledAt, List.of(), Baseline.empty());
            }
            return new Snapshot(true, "AVAILABLE", sampledAt, List.copyOf(entries), baseline(entries));
        } catch (IOException | RuntimeException ignored) {
            return Snapshot.unavailable(sampledAt, "UNAVAILABLE");
        }
    }

    private boolean isTrustedHistoryFile(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return normalized.getParent() != null
                && normalized.getParent().equals(historyDirectory)
                && Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)
                && Files.isReadable(normalized)
                && HISTORY_FILE.matcher(normalized.getFileName().toString()).matches();
    }

    private Entry readEntry(Path path) {
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            XingweiReleaseAttestationService.Attestation attestation = attestationService.parseTrusted(root);
            if (attestation == null) {
                return null;
            }
            String expectedFile = attestation.currentRelease().replace(".jar", ".json");
            if (!expectedFile.equals(path.getFileName().toString())) {
                return null;
            }
            return toEntry(attestation);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private Entry toEntry(XingweiReleaseAttestationService.Attestation attestation) {
        XingweiReleaseAttestationService.HealthComparison comparison = attestation.healthComparison();
        if (comparison == null) {
            return new Entry(
                    attestation.publishedAt(),
                    attestation.gitSha(),
                    attestation.currentRelease(),
                    attestation.previousRelease(),
                    false,
                    0,
                    0,
                    0,
                    0.0,
                    "LEGACY");
        }

        int beforeMax = Math.max(
                comparison.preDeploy8080().homeMillis(),
                comparison.preDeploy8080().servicesMillis());
        int afterMax = Math.max(
                comparison.postDeploy8080().homeMillis(),
                comparison.postDeploy8080().servicesMillis());
        int delta = afterMax - beforeMax;
        double ratio = beforeMax > 0 ? (double) afterMax / (double) beforeMax : (afterMax > 0 ? 999.0 : 1.0);
        boolean slower = delta >= XingweiOperationsAlertService.RELEASE_LATENCY_WATCH_DELTA_MILLIS
                && ratio >= XingweiOperationsAlertService.RELEASE_LATENCY_WATCH_RATIO;
        return new Entry(
                attestation.publishedAt(),
                attestation.gitSha(),
                attestation.currentRelease(),
                attestation.previousRelease(),
                true,
                beforeMax,
                afterMax,
                delta,
                finiteRatio(ratio),
                slower ? "WATCH" : "STABLE");
    }

    private Baseline baseline(List<Entry> entries) {
        List<Entry> comparable = entries.stream().filter(Entry::healthComparable).toList();
        if (comparable.isEmpty()) {
            return Baseline.empty();
        }

        List<Integer> afterValues = comparable.stream()
                .map(Entry::afterMaxMillis)
                .sorted()
                .toList();
        int medianPostMax = median(afterValues);
        long stableCount = comparable.stream().filter(entry -> "STABLE".equals(entry.state())).count();
        long watchCount = comparable.size() - stableCount;
        Entry latest = comparable.get(0);
        int latestVsMedian = latest.afterMaxMillis() - medianPostMax;
        return new Baseline(
                comparable.size() >= 2,
                comparable.size() >= 2 ? "ESTABLISHED" : "BUILDING",
                comparable.size(),
                medianPostMax,
                stableCount,
                watchCount,
                latest.afterMaxMillis(),
                latestVsMedian);
    }

    private int median(List<Integer> values) {
        int size = values.size();
        if (size == 0) {
            return 0;
        }
        if ((size & 1) == 1) {
            return values.get(size / 2);
        }
        return (int) Math.round((values.get(size / 2 - 1) + values.get(size / 2)) / 2.0);
    }

    private double finiteRatio(double ratio) {
        return Double.isFinite(ratio) ? ratio : 999.0;
    }

    public record Snapshot(
            boolean available,
            String status,
            Instant sampledAt,
            List<Entry> entries,
            Baseline baseline) {
        static Snapshot unavailable(Instant sampledAt, String status) {
            return new Snapshot(false, status, sampledAt, List.of(), Baseline.empty());
        }
    }

    public record Entry(
            Instant publishedAt,
            String gitSha,
            String currentRelease,
            String previousRelease,
            boolean healthComparable,
            int beforeMaxMillis,
            int afterMaxMillis,
            int deltaMillis,
            double ratio,
            String state) {}

    public record Baseline(
            boolean ready,
            String state,
            int comparableReleaseCount,
            int medianPostDeployMaxMillis,
            long stableReleaseCount,
            long watchReleaseCount,
            int latestPostDeployMaxMillis,
            int latestVsMedianMillis) {
        static Baseline empty() {
            return new Baseline(false, "BUILDING", 0, 0, 0, 0, 0, 0);
        }
    }
}
