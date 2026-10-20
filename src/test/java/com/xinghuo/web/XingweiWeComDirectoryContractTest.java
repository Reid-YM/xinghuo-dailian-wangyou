package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiWeComDirectoryContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void v2gIsStrictlyReadOnlyAndUsesEmployeeViewPermission() throws Exception {
        String controller = read("java/com/xinghuo/controller/XingweiWeComDirectoryController.java");
        String client = read("java/com/xinghuo/wecom/XingweiWeComDirectoryClient.java");
        String properties = read("java/com/xinghuo/wecom/XingweiWeComProperties.java");
        String app = read("resources/application.yml");

        assertTrue(controller.contains("@GetMapping(\"/directory\")"));
        assertTrue(controller.contains("'employee:view'"));
        assertFalse(controller.contains("@PostMapping"));
        assertFalse(controller.contains("@PatchMapping"));
        assertFalse(controller.contains("@PutMapping"));
        assertFalse(controller.contains("@DeleteMapping"));
        assertFalse(client.contains("/user/create"));
        assertFalse(client.contains("/user/update"));
        assertFalse(client.contains("/user/delete"));
        assertFalse(properties.contains("contactSecret"));
        assertFalse(app.contains("WECOM_SECRET"));
    }

    @Test
    void v2gDoesNotExposeEmployeeContactFieldsAndReusesV63Positions() throws Exception {
        String service = read("java/com/xinghuo/wecom/XingweiWeComDirectoryService.java");
        String js = read("resources/static/assets/admin-xingwei-wecom.js");
        String template = read("resources/templates/admin-xingwei-wecom.html");

        assertTrue(service.contains("org.positions()"));
        assertFalse(service.contains("mobile"));
        assertFalse(service.contains("email"));
        assertFalse(service.contains("biz_mail"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("insertAdjacentHTML"));
        assertTrue(template.contains("员工通讯录"));
        assertTrue(template.contains("/admin/xingwei/work"));
    }

    @Test
    void v2gHasDedicatedPageRouteConsoleEntryAndOnlyReadSecrets() throws Exception {
        String page = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String shell = read("resources/templates/admin-xingwei.html");
        String app = read("resources/application.yml");
        assertTrue(page.contains("@GetMapping(\"/admin/xingwei/wecom\")"));
        assertTrue(page.contains("return \"admin-xingwei-wecom\""));
        assertTrue(shell.contains("href=\"/admin/xingwei/wecom\""));
        assertTrue(shell.contains("岗位与企业微信员工"));
        assertTrue(app.contains("WECOM_CORP_ID"));
        assertTrue(app.contains("WECOM_APP_SECRET"));
        assertFalse(app.contains("contact-secret"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }
}
