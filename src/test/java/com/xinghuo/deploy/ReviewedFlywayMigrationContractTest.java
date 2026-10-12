package com.xinghuo.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class ReviewedFlywayMigrationContractTest {

    @Test
    void appliedV59KeepsProductionChecksumAndExactReviewedBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V59__responsive_visual_controls.sql");
        CRC32 crc = new CRC32();
        try (BufferedReader reader = Files.newBufferedReader(migration, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                crc.update(line.getBytes(StandardCharsets.UTF_8));
            }
        }
        assertEquals(1593454081, (int) crc.getValue(),
                "V59 is already applied in production and must remain immutable");

        String gitBlobSha = gitBlobSha(migration);
        assertEquals("2937c597c7bb9b9663ed73c513c2fa4e98c7aa4f", gitBlobSha,
                "historical V59 content changed after production execution");
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));
        assertTrue(manifest.contains("V59__responsive_visual_controls.sql " + gitBlobSha),
                "review manifest must bind historical V59 to its exact applied blob");
    }

    @Test
    void reviewedV60RecordMatchesExactMigrationBlobAndDeployScriptChecksIt() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V60__wechat_mini_pairing.sql");
        String gitBlobSha = gitBlobSha(migration);

        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));
        assertEquals("13403a4303243618b2b7855d7ef20f61ad6be6d9", gitBlobSha,
                "V60 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V60__wechat_mini_pairing.sql " + gitBlobSha),
                "review manifest must bind V60 to its exact git blob hash");

        String deploy = Files.readString(Path.of("scripts/deploy-from-github.sh"));
        assertTrue(deploy.contains("migration_has_exact_review"));
        assertTrue(deploy.contains("git -C \"$WORK\" hash-object"));
        assertTrue(deploy.contains("REVIEWED_FLYWAY_MIGRATIONS.txt"));
    }

    private String gitBlobSha(Path migration) throws Exception {
        byte[] content = Files.readAllBytes(migration);
        byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(header);
        sha1.update(content);
        return HexFormat.of().formatHex(sha1.digest());
    }
}
