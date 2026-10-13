package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploySmokePortRecoveryContractTest {
    private static final Path SCRIPT = Path.of("scripts/deploy-from-github.sh");

    @Test
    void staleSmokePortRecoveryOnlyTouchesKnownProjectUnitAndFailsClosed() throws Exception {
        String script = Files.readString(SCRIPT);

        assertTrue(script.contains("prepare_smoke_port()"));
        assertTrue(script.contains("cleanup_smoke"));
        assertTrue(script.contains("systemctl stop \"$SMOKE_UNIT\""));
        assertTrue(script.contains("systemctl reset-failed \"$SMOKE_UNIT\""));
        assertTrue(script.contains("ss -lntp | grep \":${SMOKE_PORT}\\\\b\""));
        assertTrue(script.contains("systemctl status \"$SMOKE_UNIT\" --no-pager"));
        assertTrue(script.contains("仅输出现场信息，不自动终止未知进程"));
        assertTrue(script.contains("prepare_smoke_port || fail \"$SMOKE_PORT 仍被未知或非本次可安全清理的进程占用\""));

        assertFalse(script.contains("pkill"), "deployment recovery must not kill arbitrary processes by pattern");
        assertFalse(script.contains("kill -9"), "deployment recovery must not SIGKILL an unknown port owner");

        int preflightRecovery = script.indexOf("prepare_smoke_port || fail");
        int mavenBuild = script.indexOf("log \"4/8 Maven 完整构建\"");
        assertTrue(preflightRecovery >= 0 && preflightRecovery < mavenBuild,
                "known smoke cleanup must happen before Maven/build work begins");
    }
}
