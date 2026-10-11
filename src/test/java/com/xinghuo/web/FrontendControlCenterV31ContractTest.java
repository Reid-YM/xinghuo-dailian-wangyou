package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendControlCenterV31ContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void serviceKeepsDraftAndPreviousOutsidePublishedNamespace() throws Exception {
        String service = Files.readString(MAIN.resolve("java/com/xinghuo/service/FrontendControlService.java"));
        assertTrue(service.contains("DRAFT_PREFIX = \"frontend.draft.\""));
        assertTrue(service.contains("PREVIOUS_PREFIX = \"frontend.previous.\""));
        assertTrue(service.contains("publishDraft"));
        assertTrue(service.contains("restorePrevious"));
        assertTrue(service.contains("stageGlobalInheritance"));
        assertTrue(service.contains("DRAFT_INHERIT_KEY"));
        assertFalse(service.contains("Flyway"));
    }

    @Test
    void adminApiExposesDraftPublishRestoreWithoutChangingPublicEndpoint() throws Exception {
        String admin = Files.readString(MAIN.resolve("java/com/xinghuo/controller/FrontendControlApiController.java"));
        String publicApi = Files.readString(MAIN.resolve("java/com/xinghuo/controller/FrontendControlPublicController.java"));
        assertTrue(admin.contains("@GetMapping(\"/workspace\")"));
        assertTrue(admin.contains("@PatchMapping(\"/draft\")"));
        assertTrue(admin.contains("@PostMapping(\"/publish\")"));
        assertTrue(admin.contains("@PostMapping(\"/restore-previous\")"));
        assertTrue(admin.contains("@PostMapping(\"/draft/inherit-global\")"));
        assertFalse(publicApi.contains("draft"));
        assertFalse(publicApi.contains("previous"));
    }

    @Test
    void adminLoadsFreshV31WorkspaceEnhancerAndKeepsV3Foundation() throws Exception {
        String template = Files.readString(MAIN.resolve("resources/templates/admin-frontend-controls.html"));
        String enhancer = Files.readString(MAIN.resolve("resources/static/assets/admin-frontend-controls-v31.js"));
        assertTrue(template.contains("前端控制中心 V3.1"));
        assertTrue(template.contains("admin-frontend-controls.js?v=20261011fc30"));
        assertTrue(template.contains("admin-frontend-controls-v31.js?v=20261011fc31"));
        assertTrue(enhancer.contains("发布到前台"));
        assertTrue(enhancer.contains("保存草稿"));
        assertTrue(enhancer.contains("恢复上一版"));
        assertTrue(enhancer.contains("实时草稿预览"));
        assertTrue(enhancer.contains("/api/admin/frontend-controls/draft"));
        assertTrue(enhancer.contains("/api/admin/frontend-controls/publish"));
        assertTrue(enhancer.contains("/api/admin/frontend-controls/restore-previous"));
        assertTrue(enhancer.contains("event.stopImmediatePropagation()"),
                "V3.1 submit/reset must prevent the legacy immediate-save path from also firing");
    }

    @Test
    void previewDoesNotEmbedArbitraryProductionPageOrWriteBusinessApis() throws Exception {
        String enhancer = Files.readString(MAIN.resolve("resources/static/assets/admin-frontend-controls-v31.js"));
        assertFalse(enhancer.contains("iframe"));
        assertFalse(enhancer.contains("/api/quotes"));
        assertFalse(enhancer.contains("/api/orders"));
        assertFalse(enhancer.contains("/api/payment"));
        assertFalse(enhancer.contains("innerHTML = fieldValue"));
    }
}
