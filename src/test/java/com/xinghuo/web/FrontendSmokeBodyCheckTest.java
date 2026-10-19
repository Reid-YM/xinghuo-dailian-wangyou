package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrontendSmokeBodyCheckTest {
    @TempDir Path temp;

    private String bodyCheckFunction() throws Exception {
        String source = Files.readString(Path.of("scripts/smoke_test.sh"));
        var matcher = Pattern.compile("(?ms)^check_body_contains\\(\\) \\{.*?^\\}").matcher(source);
        assertTrue(matcher.find(), "body check must remain part of production smoke");
        return matcher.group();
    }

    private Result run(String marker, String status) throws Exception {
        String mockCurl = """
                curl() {
                  local output=""
                  while [ "$#" -gt 0 ]; do
                    if [ "$1" = "-o" ]; then output="$2"; shift 2; else shift; fi
                  done
                  printf '<html lang="en">' > "$output"
                  if [ "$MOCK_MARKER" = "yes" ]; then printf '<script src="/next/shell.js?v=abc"></script>' >> "$output"; fi
                  head -c 131072 /dev/zero | tr '\\000' 'x' >> "$output"
                  printf '%s' "$MOCK_STATUS"
                }
                """;
        String script = "set -euo pipefail\nBASE=http://127.0.0.1:18080\nfail=0\n"
                + mockCurl + bodyCheckFunction()
                + "\ncheck_body_contains /rules-en /next/shell.js\n"
                + "check_body_contains /privacy-en /next/shell.js\n"
                + "exit \"$fail\"\n";
        Path harness = temp.resolve("check.sh");
        Files.writeString(harness, script);
        var process = new ProcessBuilder("bash", harness.toString()).redirectErrorStream(true);
        process.environment().put("MOCK_MARKER", marker);
        process.environment().put("MOCK_STATUS", status);
        var running = process.start();
        String output = new String(running.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(running.waitFor(), output);
    }

    private record Result(int code, String output) {}

    @Test void englishLegalPagesRequireActualNextMarkerInLargeResponse() throws Exception {
        Result good = run("yes", "200");
        assertEquals(0, good.code(), good.output());
        assertTrue(good.output().contains("OK   /rules-en contains=/next/shell.js"));
        assertTrue(good.output().contains("OK   /privacy-en contains=/next/shell.js"));
    }

    @Test void missingRuntimeOrNonSuccessHttpMustFailClosed() throws Exception {
        Result absent = run("no", "200");
        assertNotEquals(0, absent.code());
        assertTrue(absent.output().contains("http=200 body_bytes="));
        Result rejected = run("yes", "429");
        assertNotEquals(0, rejected.code());
        assertTrue(rejected.output().contains("http=429 body_bytes="));
    }
}
