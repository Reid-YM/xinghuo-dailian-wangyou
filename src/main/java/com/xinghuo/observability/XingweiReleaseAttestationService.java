package com.xinghuo.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 读取由生产发布脚本在全部冒烟通过后原子落盘的主机侧发布凭证。
 *
 * <p>应用只读该凭证，不自行推断 Nginx、systemd、release 链或发布是否成功。
 * 缺失、损坏或格式不符合约束时，只返回不可用状态，不外泄文件路径或解析异常。
 */
@Service
public class XingweiReleaseAttestationService {
    private static final String TRUSTED_SOURCE = "production-host-gateway";
    private static final int EXPECTED_MAIN_PORT = 8080;
    private static final int EXPECTED_CANARY_PORT = 8081;
    private static final Pattern GIT_SHA = Pattern.compile("^[0-9a-f]{40}$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern RELEASE = Pattern.compile("^xinghuo-v2-standard-[0-9]{14}\\.jar$");

    private final ObjectMapper objectMapper;
    private final Path attestationPath;

    public XingweiReleaseAttestationService(
            ObjectMapper objectMapper,
            @Value("${xinghuo.release-attestation.path:/opt/xinghuo-v2/release-attestation.json}") String attestationPath) {
        this.objectMapper = objectMapper;
        this.attestationPath = Path.of(attestationPath).toAbsolutePath().normalize();
    }

    public Snapshot snapshot() {
        Instant sampledAt = Instant.now();
        if (!Files.isRegularFile(attestationPath) || !Files.isReadable(attestationPath)) {
            return Snapshot.unavailable(sampledAt, "MISSING");
        }
        try {
            JsonNode root = objectMapper.readTree(attestationPath.toFile());
            Attestation attestation = parse(root);
            if (attestation == null) {
                return Snapshot.unavailable(sampledAt, "INVALID");
            }
            return new Snapshot(true, "AVAILABLE", sampledAt, attestation);
        } catch (IOException | RuntimeException ignored) {
            return Snapshot.unavailable(sampledAt, "INVALID");
        }
    }

    private Attestation parse(JsonNode root) {
        if (root == null || root.path("schemaVersion").asInt(-1) != 1) {
            return null;
        }
        String source = root.path("source").asText("");
        String gitSha = root.path("gitSha").asText("");
        String jarSha256 = root.path("jarSha256").asText("");
        String currentRelease = root.path("currentRelease").asText("");
        String previousRelease = root.path("previousRelease").asText("");
        int mainPort = root.path("mainPort").asInt(-1);
        int canaryPort = root.path("canaryPort").asInt(-1);
        int nginxUpstreamPort = root.path("nginxUpstreamPort").asInt(-1);
        boolean canaryStopped = root.path("canaryStopped").asBoolean(false);
        JsonNode checks = root.path("checks");
        boolean predeployPassed = checks.path("predeploy18080").asBoolean(false);
        boolean canaryPassed = checks.path("canary8081").asBoolean(false);
        boolean productionPassed = checks.path("production8080").asBoolean(false);
        Instant publishedAt = parseInstant(root.path("publishedAt").asText(""));

        if (!TRUSTED_SOURCE.equals(source)
                || publishedAt == null
                || !GIT_SHA.matcher(gitSha).matches()
                || !SHA256.matcher(jarSha256).matches()
                || !RELEASE.matcher(currentRelease).matches()
                || !RELEASE.matcher(previousRelease).matches()
                || currentRelease.equals(previousRelease)
                || mainPort != EXPECTED_MAIN_PORT
                || canaryPort != EXPECTED_CANARY_PORT
                || nginxUpstreamPort != EXPECTED_MAIN_PORT
                || !canaryStopped
                || !predeployPassed
                || !canaryPassed
                || !productionPassed) {
            return null;
        }

        return new Attestation(
                publishedAt,
                gitSha,
                jarSha256,
                currentRelease,
                previousRelease,
                mainPort,
                canaryPort,
                nginxUpstreamPort,
                canaryStopped,
                new Checks(predeployPassed, canaryPassed, productionPassed));
    }

    private Instant parseInstant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public record Snapshot(boolean available, String status, Instant sampledAt, Attestation attestation) {
        static Snapshot unavailable(Instant sampledAt, String status) {
            return new Snapshot(false, status, sampledAt, null);
        }
    }

    public record Attestation(
            Instant publishedAt,
            String gitSha,
            String jarSha256,
            String currentRelease,
            String previousRelease,
            int mainPort,
            int canaryPort,
            int nginxUpstreamPort,
            boolean canaryStopped,
            Checks checks) {}

    public record Checks(boolean predeploy18080, boolean canary8081, boolean production8080) {}
}
