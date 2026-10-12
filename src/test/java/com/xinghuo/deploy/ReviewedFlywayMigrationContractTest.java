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
    void appliedHistoricalMigrationsRemainImmutable() throws Exception {
        assertHistoricalMigration(
                "V59__responsive_visual_controls.sql",
                1593454081,
                "2937c597c7bb9b9663ed73c513c2fa4e98c7aa4f");
        assertHistoricalMigration(
                "V60__responsive_component_controls.sql",
                1507001770,
                "87e1c4a434ef1adfe9b7b259f8fc97c726b66cfa");
        assertHistoricalMigration(
                "V61__mobile_background_image_controls.sql",
                413343084,
                "8fe701049f783178be34c156b141b87b149afee3");
    }

    @Test
    void reviewedV62RecordMatchesExactMigrationBlobAndDeployScriptChecksIt() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V62__wechat_mini_pairing.sql");
        String gitBlobSha = gitBlobSha(migration);

        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));
        assertEquals("13403a4303243618b2b7855d7ef20f61ad6be6d9", gitBlobSha,
                "V62 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V62__wechat_mini_pairing.sql " + gitBlobSha),
                "review manifest must bind V62 to its exact git blob hash");

        String deploy = Files.readString(Path.of("scripts/deploy-from-github.sh"));
        assertTrue(deploy.contains("migration_has_exact_review"));
        assertTrue(deploy.contains("git -C \"$WORK\" hash-object"));
        assertTrue(deploy.contains("REVIEWED_FLYWAY_MIGRATIONS.txt"));
    }

    @Test
    void reviewedV63OrganizationFoundationMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V63__xingwei_org_rbac_foundation.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("cc2a2f1e844a16b69aa439a2cd88fdeaabd7b69d", gitBlobSha,
                "V63 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V63__xingwei_org_rbac_foundation.sql " + gitBlobSha),
                "review manifest must bind V63 to its exact git blob hash");
    }

    private void assertHistoricalMigration(String filename, int expectedChecksum, String expectedBlobSha) throws Exception {
        Path migration = Path.of("src/main/resources/db/migration", filename);
        CRC32 crc = new CRC32();
        try (BufferedReader reader = Files.newBufferedReader(migration, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                crc.update(line.getBytes(StandardCharsets.UTF_8));
            }
        }
        assertEquals(expectedChecksum, (int) crc.getValue(),
                filename + " is an already-used historical migration and must remain immutable");

        String gitBlobSha = gitBlobSha(migration);
        assertEquals(expectedBlobSha, gitBlobSha,
                filename + " content changed after historical merge/application");
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));
        assertTrue(manifest.contains(filename + " " + gitBlobSha),
                "review manifest must bind " + filename + " to its exact historical blob");
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
