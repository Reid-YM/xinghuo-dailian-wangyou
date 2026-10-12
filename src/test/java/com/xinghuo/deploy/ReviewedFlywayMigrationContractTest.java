package com.xinghuo.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ReviewedFlywayMigrationContractTest {

    @Test
    void reviewedV59RecordMatchesExactMigrationBlobAndDeployScriptChecksIt() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V59__wechat_mini_pairing.sql");
        byte[] content = Files.readAllBytes(migration);
        byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(header);
        sha1.update(content);
        String gitBlobSha = HexFormat.of().formatHex(sha1.digest());

        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));
        assertEquals("13403a4303243618b2b7855d7ef20f61ad6be6d9", gitBlobSha,
                "V59 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V59__wechat_mini_pairing.sql " + gitBlobSha),
                "review manifest must bind V59 to its exact git blob hash");

        String deploy = Files.readString(Path.of("scripts/deploy-from-github.sh"));
        assertTrue(deploy.contains("migration_has_exact_review"));
        assertTrue(deploy.contains("git -C \"$WORK\" hash-object"));
        assertTrue(deploy.contains("REVIEWED_FLYWAY_MIGRATIONS.txt"));
    }
}
