package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LocalizedLegalSeoContractTest {

    @Test
    void localizedLegalPagesHaveSelfCanonicalsAndReciprocalHreflang() throws Exception {
        String rulesZh = read("src/main/resources/templates/rules.html");
        String rulesEn = read("src/main/resources/templates/rules-en.html");
        String privacyZh = read("src/main/resources/templates/privacy.html");
        String privacyEn = read("src/main/resources/templates/privacy-en.html");

        assertPair(rulesZh, "https://wydlgf.cn/rules", "https://wydlgf.cn/rules-en");
        assertPair(rulesEn, "https://wydlgf.cn/rules-en", "https://wydlgf.cn/rules");
        assertPair(privacyZh, "https://wydlgf.cn/privacy", "https://wydlgf.cn/privacy-en");
        assertPair(privacyEn, "https://wydlgf.cn/privacy-en", "https://wydlgf.cn/privacy");
    }

    @Test
    void canonicalTargetsAndLanguageSwitchArePublicRoutes() throws Exception {
        String security = read("src/main/java/com/xinghuo/security/SecurityConfig.java");
        assertTrue(security.contains("\"/rules-en\""));
        assertTrue(security.contains("\"/privacy-en\""));
        assertTrue(security.contains("\"/language\""));
    }

    private void assertPair(String html, String canonical, String alternate) {
        assertTrue(html.contains("rel=\"canonical\" href=\"" + canonical + "\""),
                "missing self canonical: " + canonical);
        assertTrue(html.contains("hreflang=\"zh-CN\""), "missing zh-CN alternate");
        assertTrue(html.contains("hreflang=\"en\""), "missing en alternate");
        assertTrue(html.contains("href=\"" + alternate + "\""),
                "missing reciprocal alternate: " + alternate);
    }

    private String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
