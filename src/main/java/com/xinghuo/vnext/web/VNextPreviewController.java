package com.xinghuo.vnext.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * VNext 前端隔离预览入口。
 *
 * <p>当前只把 /vnext 与 /vnext/ 转发到 React 构建产物，不接管任何现有公开页面。
 * 用户确认 UI 体系之后，主入口切换会通过单独 PR 完成。</p>
 */
@Controller
public class VNextPreviewController {

    @GetMapping({"/vnext", "/vnext/"})
    public String preview() {
        return "forward:/vnext/index.html";
    }
}
