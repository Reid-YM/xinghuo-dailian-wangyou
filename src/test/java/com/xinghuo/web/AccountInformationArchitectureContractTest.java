package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AccountInformationArchitectureContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void accountHomeIsOverviewInsteadOfMegaTabPage() throws IOException {
        String account = read("resources/templates/account.html");
        String vnext = Files.readString(Path.of("frontend-vnext/src/main.tsx"));
        assertTrue(account.contains("data-page=\"account\""));
        assertTrue(account.contains("id=\"root\""));
        assertTrue(account.contains("/vnext/assets/app.js"));
        assertTrue(vnext.contains("'/account/requests'"));
        assertTrue(vnext.contains("'/account/orders'"));
        assertTrue(vnext.contains("'/account/progress'"));
        assertTrue(vnext.contains("'/account/settings'"));
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
        assertTrue(controller.contains("/console/overview"));
        assertTrue(controller.contains("/console/profile"));
        assertTrue(controller.contains("redirect:/account/requests"));
        assertTrue(controller.contains("redirect:/account/orders"));
        assertTrue(controller.contains("redirect:/account/progress"));
        assertTrue(controller.contains("redirect:/account/settings#security"));
        assertTrue(controller.contains("redirect:/contact"));
        assertFalse(console.contains("console-tabs"));
        assertFalse(console.contains("data-console-panel"));
        assertTrue(script.contains("quotes: '/account/requests'"));
        assertTrue(script.contains("orders: '/account/orders'"));
        assertTrue(script.contains("security: '/account/settings#security'"));
    }

    @Test
    void legacyAccountPathsAndHashesResolveOnlyToCanonicalUserPages() throws IOException {
        String controller = read("java/com/xinghuo/account/web/AccountCenterPageController.java");
        String script = read("resources/static/assets/account-center.js");
        assertTrue(controller.contains("/account/quotes"));
        assertTrue(controller.contains("/account/profile"));
        assertTrue(controller.contains("/account/contact"));
        assertTrue(controller.contains("/account/security"));
        assertTrue(controller.contains("/account/devices"));
        assertTrue(controller.contains("redirect:/account/requests"));
        assertTrue(controller.contains("redirect:/account/settings#profile"));
        assertTrue(controller.contains("redirect:/account/settings#security"));
        assertTrue(script.contains("quotes: '/account/requests'"));
        assertTrue(script.contains("orders: '/account/orders'"));
        assertTrue(script.contains("profile: '/account/settings#profile'"));
        assertTrue(script.contains("security: '/account/settings#security'"));
    }

    @Test
    void userFacingAccountSurfacesNeverLinkToAdmin() throws IOException {
        Path templates = ROOT.resolve("resources/templates");
        try (Stream<Path> files = Files.list(templates)) {
            files.filter(path -> path.getFileName().toString().startsWith("account"))
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    .forEach(path -> assertNoAdminDestination(path));
        }
        assertNoAdminDestination(ROOT.resolve("resources/templates/fragments/account-center.html"));
        assertNoAdminDestination(ROOT.resolve("resources/templates/console.html"));
        assertNoAdminDestination(ROOT.resolve("resources/static/assets/account-center.js"));
        assertNoAdminDestination(ROOT.resolve("resources/static/assets/account.js"));
        assertNoAdminDestination(ROOT.resolve("resources/static/assets/console.js"));
        String center = read("resources/static/assets/account-center.js");
        assertTrue(center.contains("href === '/admin' || href.startsWith('/admin/')"));
        assertTrue(center.contains("location.assign('/account')"));
    }

    @Test
    void productionSmokeCoversEveryAccountDestinationThatUsersCanClick() throws IOException {
        String smoke = Files.readString(Path.of("scripts/smoke_test.sh"));
        for (String path : new String[]{
                "/account/requests", "/account/orders", "/account/progress", "/account/settings", "/account/submit",
                "/account/quotes", "/account/profile", "/account/security",
                "/console/overview", "/console/profile", "/console/quotes", "/console/orders", "/console/security"}) {
            assertTrue(smoke.contains("check_code " + path + " 302"), "missing smoke coverage for " + path);
        }
    }

    @Test
    void sharedNavigationUsesCanonicalAccountEntry() throws IOException {
        String site = read("resources/templates/fragments/site.html");
        String settings = read("java/com/xinghuo/service/SiteSettingService.java");
        String controls = read("resources/static/assets/global-ui-controls.js");
        assertTrue(site.contains("href=\"/account\" data-account-entry"));
        assertTrue(site.contains("href=\"/account\" data-global-ui=\"footer-console\""));
        assertFalse(site.contains("href=\"/console\" data-global-ui=\"footer-console\""));
        assertTrue(settings.contains("Map.entry(\"navAccountLabel\", \"个人中心\")"));
        assertTrue(settings.contains("Map.entry(\"footerConsoleLabel\", \"个人中心\")"));
        assertTrue(settings.contains("normalizeLegacyAccountLabels(out)"));
        assertTrue(controls.contains("['/account', settings.navAccountLabel || '个人中心'"));
        assertFalse(controls.contains("['/account/login', settings.navAccountLabel"));
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

    @Test
    void settingsRailReusesExistingProfileBinding() throws IOException {
        String fragment = read("resources/templates/fragments/account-center.html");
        assertTrue(fragment.contains("data-profile-avatar-preview"));
        assertTrue(fragment.contains("data-settings-name"));
        assertTrue(fragment.contains("data-settings-contact"));
    }

    private static void assertNoAdminDestination(Path path) {
        try {
            String content = Files.readString(path);
            assertFalse(content.contains("href=\"/admin"), "user page contains admin href: " + path);
            assertFalse(content.contains("location.href='/admin"), "user script contains admin redirect: " + path);
            assertFalse(content.contains("location.assign('/admin"), "user script contains admin redirect: " + path);
            assertFalse(content.contains("location.replace('/admin"), "user script contains admin redirect: " + path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
