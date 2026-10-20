package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrontendDeploymentRollbackTest {
    @TempDir Path temp;
    String function(String name)throws Exception {
        String source=Files.readString(Path.of("scripts/deploy-zero-downtime.sh"));
        var matcher=Pattern.compile("(?ms)^"+name+"\\(\\) \\{.*?^}").matcher(source);
        assertTrue(matcher.find(),name);return matcher.group();
    }
    int run(String script,Map<String,String> environment)throws Exception {
        Path harness=Files.createTempFile(temp,"harness-",".sh");Files.writeString(harness,"set -euo pipefail\n"+script);
        var process=new ProcessBuilder("bash",harness.toString()).redirectErrorStream(true);
        process.environment().putAll(environment);var running=process.start();
        String output=new String(running.getInputStream().readAllBytes());int result=running.waitFor();
        Files.writeString(temp.resolve("last-output"),output);return result;
    }
    @Test void previousModeProbeRejectsUnknownViewsAndTransportFailures()throws Exception {
        String probe="curl(){ if [ \"${CURL_FAIL:-0}\" = 1 ]; then return 7; fi; printf '%b' \"$HEADERS\"; }\n"+function("frontend_mode")+"\nfrontend_mode 8080\n";
        assertEquals(0,run(probe,Map.of("HEADERS","HTTP/1.1 200 OK\r\nX-Xinghuo-Frontend: next\r\n\r\n")));
        assertEquals("next\n",Files.readString(temp.resolve("last-output")));
        assertEquals(0,run(probe,Map.of("HEADERS","HTTP/1.1 200 OK\r\n\r\n")));
        assertEquals("legacy\n",Files.readString(temp.resolve("last-output")));
        assertNotEquals(0,run(probe,Map.of("HEADERS","X-Xinghuo-Frontend: unknown\r\n")));
        assertNotEquals(0,run(probe,Map.of("HEADERS","","CURL_FAIL","1")));
    }
    @Test void rollbackUsesPreviousLegacyModeWhileCandidateStillRequiresNext()throws Exception {
        Path output=temp.resolve("smokes");Path smoke=temp.resolve("smoke.sh");
        Files.writeString(smoke,"printf '%s:%s\\n' \"$FRONTEND_EXPECTED_MODE\" \"$1\" >> \"$CAPTURE\"\n");
        String harness="""
            log(){ :; }
            fail(){ exit 1; }
            sudo(){ :; }
            wait_healthy(){ return 0; }
            switch_upstream(){ printf 'upstream:%s\\n' "$1" >> "$CAPTURE"; }
            MAIN_PORT=8080
            SERVICE=dummy.service
            PREVIOUS_JAR=/dummy/previous.jar
            APP_LINK=/dummy/current.jar
            PREVIOUS_FRONTEND_MODE=legacy
            """+function("run_smoke")+"\n"+function("rollback_main")+"\nrun_smoke 8081\nrollback_main\n";
        int result=run(harness,Map.of("SMOKE_TEST",smoke.toString(),"CAPTURE",output.toString(),"FRONTEND_EXPECTED_MODE","next"));
        assertEquals(0,result,Files.readString(temp.resolve("last-output")));
        assertEquals(List.of("next:8081","legacy:8080","upstream:8080"),Files.readAllLines(output));
    }
}
