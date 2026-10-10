package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PageRouteCompatibilityContractTest {
    @Test
    void migratedPageControllersKeepLegacyRoutesAndTemplates() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/com/xinghuo/catalog/web/PublicCatalogPageController.java"));
        String account = Files.readString(Path.of("src/main/java/com/xinghuo/account/web/AccountPageController.java"));
        String order = Files.readString(Path.of("src/main/java/com/xinghuo/order/web/OrderPageController.java"));
        String quote = Files.readString(Path.of("src/main/java/com/xinghuo/quote/web/PublicQuotePageController.java"));
        String staticPages = Files.readString(Path.of("src/main/java/com/xinghuo/page/web/StaticPageController.java"));
        String admin = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/web/AdminPageController.java"));
        String mastery = Files.readString(Path.of("src/main/java/com/xinghuo/toolstation/web/MasteryPageController.java"));

        assertThat(catalog)
                .contains("\"/\"")
                .contains("\"/services\"")
                .contains("\"/service/{slug}\"")
                .contains("\"/game/{seoSlug}\"")
                .contains("return \"index\"")
                .contains("return \"services\"")
                .contains("return \"service-detail\"")
                .contains("return \"game\"");
        assertThat(account)
                .contains("/account/login", "/account/register", "/account/settings", "/account/submit", "/account/reset");
        assertThat(order)
                .contains("/records", "/record/{businessNo}", "/order/{orderNo}", "record-detail");
        assertThat(quote).contains("/quote", "return \"quote\"");
        assertThat(staticPages).contains("/contact", "/rules", "/privacy", "/rules-en", "/privacy-en", "/trust");
        assertThat(admin).contains("/admin/login", "\"/admin\"", "csrf", "username");
        assertThat(mastery).contains("/mastery", "mw4-camo-tracker");
    }
}
