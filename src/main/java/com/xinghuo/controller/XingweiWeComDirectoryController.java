package com.xinghuo.controller;

import com.xinghuo.wecom.XingweiWeComDirectoryService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** V2G 企业微信通讯录只读 API。 */
@RestController
@RequestMapping("/api/admin/xingwei/wecom")
public class XingweiWeComDirectoryController {
    private final XingweiWeComDirectoryService directory;

    public XingweiWeComDirectoryController(XingweiWeComDirectoryService directory) {
        this.directory = directory;
    }

    @GetMapping("/directory")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'employee:view')")
    public Map<String, Object> directory(Authentication authentication) {
        return directory.snapshot();
    }
}
