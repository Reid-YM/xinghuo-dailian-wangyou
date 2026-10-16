package com.xinghuo.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class ReviewedFlywayMigrationContractTest {

    @Test
    void reviewedV75ChannelOperationsMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V75__xingwei_channel_operations.sql");
        String blob = gitBlobSha(migration);
        assertEquals("ba2638d5b1b0354fd133c67a43cbbc38ee95745d", blob);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));
        assertTrue(manifest.contains("V75__xingwei_channel_operations.sql " + blob));
    }

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

    @Test
    void reviewedV64BusinessOwnershipMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V64__xingwei_business_ownership.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("df0a0867fa73adda18b44b919541972a75f5b32d", gitBlobSha,
                "V64 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V64__xingwei_business_ownership.sql " + gitBlobSha),
                "review manifest must bind V64 to its exact git blob hash");
    }

    @Test
    void reviewedV65SessionSlidingRenewalMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V65__session_sliding_renewal.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("530018c0b4a099443123dcec71a05131d3cc0ac5", gitBlobSha,
                "V65 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V65__session_sliding_renewal.sql " + gitBlobSha),
                "review manifest must bind V65 to its exact git blob hash");
    }

    @Test
    void reviewedV68WeComBindingMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V68__xingwei_wecom_user_binding.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("892b44827338a3c512b1b9a87beab17526761cb7", gitBlobSha,
                "V68 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V68__xingwei_wecom_user_binding.sql " + gitBlobSha),
                "review manifest must bind V68 to its exact git blob hash");
    }

    @Test
    void reviewedV69WeComAutoRoleTrackingMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V69__xingwei_wecom_auto_role_tracking.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("b2faeff041b1f6fb14863da061e725f021f07e50", gitBlobSha,
                "V69 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V69__xingwei_wecom_auto_role_tracking.sql " + gitBlobSha),
                "review manifest must bind V69 to its exact git blob hash");
    }

    @Test
    void reviewedV70AfterSalesLedgerMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V70__customer_order_after_sales_records.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("20dd7b16b37cac737129abbc92704fec8836a7fc", gitBlobSha,
                "V70 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V70__customer_order_after_sales_records.sql " + gitBlobSha),
                "review manifest must bind V70 to its exact git blob hash");
    }

    @Test
    void reviewedV71AfterSalesCaseStateMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V71__customer_order_after_sales_case_state.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("1cc9235f8158a26c1725e8b41a96d811bc196e14", gitBlobSha,
                "V71 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V71__customer_order_after_sales_case_state.sql " + gitBlobSha),
                "review manifest must bind V71 to its exact git blob hash");
    }

    @Test
    void reviewedV72AfterSalesCaseEventsMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V72__customer_order_after_sales_case_events.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("4f5f07f11120108a8484c5e41ad86d767d1d4720", gitBlobSha,
                "V72 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V72__customer_order_after_sales_case_events.sql " + gitBlobSha),
                "review manifest must bind V72 to its exact git blob hash");
    }

    @Test
    void reviewedV73CustomerFollowupsMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V73__customer_order_after_sales_case_followups.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("b016c17b76e89a9361689c0081418a0364e1b796", gitBlobSha,
                "V73 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V73__customer_order_after_sales_case_followups.sql " + gitBlobSha),
                "review manifest must bind V73 to its exact git blob hash");
    }

    @Test
    void reviewedV74FollowupTriageCursorMatchesExactMigrationBlob() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V74__after_sales_followup_triage_cursor.sql");
        String gitBlobSha = gitBlobSha(migration);
        String manifest = Files.readString(Path.of("docs/REVIEWED_FLYWAY_MIGRATIONS.txt"));

        assertEquals("6b092df3b4656d3f171b558bc0cf6f445056911c", gitBlobSha,
                "V74 content changed after review; re-review and update the exact blob hash");
        assertTrue(manifest.contains("V74__after_sales_followup_triage_cursor.sql " + gitBlobSha),
                "review manifest must bind V74 to its exact git blob hash");
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

    /**
     * 迁移文件的 git blob sha。
     *
     * <p>仓库在 Windows 上按 CRLF 检出（core.autocrlf），但 git 提交进去的是 LF 版本，
     * 生产部署脚本也是在 Linux 工作区上跑 {@code git hash-object}。所以这里先按 LF 归一化再算：
     * 同一次提交在 Windows 与 Linux 上得到同一个（也就是真正落库的）blob sha，
     * 同时依然能挡住任何内容改动。
     */
    private String gitBlobSha(Path migration) throws Exception {
        byte[] content = normalizeLineEndings(Files.readAllBytes(migration));
        byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(header);
        sha1.update(content);
        return HexFormat.of().formatHex(sha1.digest());
    }

    /** 把 CRLF 归一化成 LF，让 blob sha 不受工作区换行符影响。 */
    private static byte[] normalizeLineEndings(byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
        for (int i = 0; i < content.length; i++) {
            if (content[i] == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                continue;
            }
            out.write(content[i]);
        }
        return out.toByteArray();
    }
}
