package com.xinghuo.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VNextPublicFinishV4ContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void sharedShellLoadsFinalPublicLayerWithStableCacheKey() throws IOException {
        String fragment = read("src/main/resources/templates/fragments/site.html");

        assertTrue(fragment.contains("/assets/vnext-public-finish-v4.css?v=20261010ui04"));
        assertTrue(fragment.contains("/assets/site-experience-v2.css"));
        assertTrue(fragment.contains("/assets/site-shell.js"));
        assertTrue(fragment.contains("/assets/global-ui-controls.js"));
    }

    @Test
    void finalPublicLayerCoversRemainingLegacyPublicSurfaces() throws IOException {
        String css = read("src/main/resources/static/assets/vnext-public-finish-v4.css");

        for (String page : List.of(
                "account-login",
                "account-register",
                "account-reset",
                "guides",
                "guide-detail",
                "mastery",
                "tool-station",
                "mw4-mastery",
                "rules",
                "privacy",
                "contact",
                "trust",
                "order")) {
            assertTrue(css.contains("body[data-page=\"" + page + "\"]"), "missing final styling scope for " + page);
        }

        assertTrue(css.contains("@media(min-width:1920px)"));
        assertTrue(css.contains("@media(min-width:2400px)"));
        assertTrue(css.contains("@media(max-width:767px)"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("html.is-wechat"));
        assertTrue(css.contains("prefers-reduced-motion"));
        assertFalse(css.contains("/api/"));
        assertFalse(css.contains("@import"));
    }

    @Test
    void targetTemplatesKeepExistingBusinessStructureAndUseSharedShell() throws IOException {
        for (String name : List.of(
                "account-login.html",
                "account-register.html",
                "account-reset.html",
                "guides.html",
                "rules.html",
                "privacy.html",
                "contact.html",
                "trust.html",
                "order.html")) {
            String html = read("src/main/resources/templates/" + name);
            assertTrue(html.contains("fragments/site :: header"), name + " must keep shared public header");
            assertTrue(html.contains("fragments/site :: footer"), name + " must keep shared public footer");
        }

        String login = read("src/main/resources/templates/account-login.html");
        String register = read("src/main/resources/templates/account-register.html");
        String order = read("src/main/resources/templates/order.html");
        String contact = read("src/main/resources/templates/contact.html");

        assertTrue(login.contains("id=\"accountLoginForm\""));
        assertTrue(register.contains("id=\"accountRegisterForm\""));
        assertTrue(order.contains("id=\"orderLookupForm\""));
        assertTrue(contact.contains("data-wecom-entry"));
    }
}
