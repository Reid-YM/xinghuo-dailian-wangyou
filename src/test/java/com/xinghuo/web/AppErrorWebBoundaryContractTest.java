package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppErrorWebBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void errorEntryPointIsFeatureOwnedAndKeepsStatusAndRequestIdContract() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/error/web/AppErrorController.java"));

        assertTrue(source.contains("package com.xinghuo.error.web;"));
        assertTrue(source.contains("implements ErrorController"));
        assertTrue(source.contains("@RequestMapping(value={\"/error\"})"));
        assertTrue(source.contains("response.setStatus(code)"));
        assertTrue(source.contains("model.addAttribute(\"requestId\", request.getAttribute(\"requestId\"))"));
        assertTrue(source.contains("request.getAttribute(\"jakarta.servlet.error.status_code\")"));
        assertTrue(source.contains("code >= 400 && code <= 599 ? code : 500"));
        assertTrue(source.contains("return \"error\";"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/AppErrorController.java")));
    }
}
