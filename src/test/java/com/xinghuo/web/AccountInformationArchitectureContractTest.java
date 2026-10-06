package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccountInformationArchitectureContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void accountHomeIsOverviewInsteadOfMegaTabPage() throws IOException {
        String account = read("resources/templates/account.html");
        assertTrue(account.contains("data-page=\"account-center-overview\""));
        assertTrue(account.contains("href=\"/account/requests\""));
        assertTrue(account.contains("href=\"/account/orders\""));
        assertTrue(account.contains("href=\"/account/progress\""));
        assertFalse(account.contains("role=\"tablist\""));
        assertFalse(account.contains("id=\"panel-orders\""));
        assertFalse(account.contains("id=\"panel-devices\""));
    }

    @Test
    void businessResponsibilitiesHaveDedicatedPages() throws IOException {
        String controller = read("java/com/xinghuo/account/web/AccountCenterPageController.java");
        assertTrue(controller.contains("@GetMapping(\"/account/requests\")"));
        assertTrue(controller.contains("@GetMapping(\"/account/orders\")"));
        assertTrue(controller.contains("@GetMapping(\"/account/progress\")"));
        assertTrue(read("resources/templates/account-requests.html").contains("data-page=\"account-center-requests\""));
        assertTrue(read("resources/templates/account-orders.html").contains("data-page=\"account-center-orders\""));
        assertTrue(read("resources/templates/account-progress.html").contains("data-page=\"account-center-progress\""));
    }

    @Test
    void settingsSeparatesProfileContactAndSecurityWithoutLongAccordion() throws IOException {
        String settings = read("resources/templates/account-settings.html");
        assertTrue(settings.contains("data-settings-view=\"profile\""));
        assertTrue(settings.contains("data-settings-view=\"contact\""));
        assertTrue(settings.contains("data-settings-view=\"security\""));
        assertTrue(settings.contains("data-settings-target=\"profile\""));
        assertTrue(settings.contains("data-settings-target=\"contact\""));
        assertTrue(settings.contains("data-settings-target=\"security\""));
        assertTrue(settings.contains("/assets/account-center.js"));
    }

    @Test
    void legacyConsoleOnlyRedirectsToCanonicalAccountDestinations() throws IOException {
        String controller = read("java/com/xinghuo/account/web/ConsolePageController.java");
        String console = read("resources/templates/console.html");
        String script = read("resources/static/assets/console.js");
        assertTrue(controller.contains("redirect:/account/requests"));
        assertTrue(controller.contains("redirect:/account/orders"));
        assertTrue(controller.contains("redirect:/account/settings#security"));
        assertTrue(controller.contains("redirect:/contact"));
        assertFalse(console.contains("console-tabs"));
        assertFalse(console.contains("data-console-panel"));
        assertTrue(script.contains("quotes: '/account/requests'"));
        assertTrue(script.contains("orders: '/account/orders'"));
        assertTrue(script.contains("security: '/account/settings#security'"));
    }

    @Test
    void sharedNavigationUsesCanonicalAccountEntry() throws IOException {
        String site = read("resources/templates/fragments/site.html");
        assertTrue(site.contains("href=\"/account\" data-account-entry"));
        assertTrue(site.contains("href=\"/account\" data-global-ui=\"footer-console\""));
        assertFalse(site.contains("href=\"/console\" data-global-ui=\"footer-console\""));
    }

    @Test
    void accountLayoutConstrainsWideScreensAndCollapsesResponsively() throws IOException {
        String css = read("resources/static/assets/account-center.css");
        assertTrue(css.contains("width:min(1480px"));
        assertTrue(css.contains("grid-template-columns:260px minmax(0,1fr)"));
        assertTrue(css.contains("@media(max-width:860px)"));
        assertTrue(css.contains("grid-template-columns:1fr"));
        assertTrue(css.contains("min-width:0"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
