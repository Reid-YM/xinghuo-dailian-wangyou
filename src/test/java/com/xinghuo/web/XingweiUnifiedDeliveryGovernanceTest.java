package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XingweiUnifiedDeliveryGovernanceTest {
    @TempDir Path root;
    @Test
    void actualValidatorAcceptsPackageDeliveryButRejectsInventedDeploymentEvidenceAndUnknownStages() throws Exception {
        for (String file : new String[]{"scripts/validate-xingwei-release-state.sh", "src/main/java/com/xinghuo/controller/XingweiAdminController.java", "src/main/resources/templates/admin-xingwei.html", "src/main/resources/templates/admin-xingwei-releases.html", ".github/workflows/ci.yml", ".github/workflows/deploy-production.yml"}) {
            Path target = root.resolve(file); Files.createDirectories(target.getParent()); Files.copy(Path.of(file), target);
        }
        assertEquals(0, validate());
        Path initialDashboard = root.resolve("src/main/resources/templates/admin-xingwei.html");
        String initialPage = Files.readString(initialDashboard);
        Files.writeString(initialDashboard,initialPage.replace("data-runtime-stage=${consoleStage}","data-runtime-stage=UNKNOWN"));
        assertNotEquals(0,validate(),"Chinese presentation must retain the actual machine stage for runtime checks");
        Files.writeString(initialDashboard,initialPage);
        Path controller = root.resolve("src/main/java/com/xinghuo/controller/XingweiAdminController.java");
        String original = Files.readString(controller);
        Files.writeString(controller, original.replace("\"V6B\", \"本版本\"", "\"V6B\", \"已上线\""));
        assertNotEquals(0, validate(), "package delivery cannot pretend to be a verified live release");
        Files.writeString(controller, original.replace("V6B · UNIFIED DELIVERY", "V6B · UNKNOWN STAGE"));
        Path dashboard = root.resolve("src/main/resources/templates/admin-xingwei.html");
        Files.writeString(dashboard, Files.readString(dashboard).replace("V6B · UNIFIED DELIVERY", "V6B · UNKNOWN STAGE"));
        assertNotEquals(0, validate(), "an unknown stage must not bypass release-state checks");
    }
    private int validate() throws Exception {
        Process process = new ProcessBuilder("bash", "scripts/validate-xingwei-release-state.sh").directory(root.toFile()).redirectErrorStream(true).redirectOutput(root.resolve("validator.log").toFile()).start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        return process.exitValue();
    }
}
