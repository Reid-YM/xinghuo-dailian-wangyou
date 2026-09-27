/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.controller.AppErrorController
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  org.springframework.boot.web.servlet.error.ErrorController
 *  org.springframework.stereotype.Controller
 *  org.springframework.ui.Model
 *  org.springframework.web.bind.annotation.RequestMapping
 */
package com.xinghuo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController
implements ErrorController {
    @RequestMapping(value={"/error"})
    public String error(HttpServletRequest request, HttpServletResponse response, Model model) {
        int code = this.statusCode(request);
        response.setStatus(code);
        boolean notFound = code == 404;
        model.addAttribute("statusCode", (Object)code);
        model.addAttribute("requestId", request.getAttribute("requestId"));
        model.addAttribute("statusLabel", (Object)(notFound ? "404" : String.valueOf(code)));
        model.addAttribute("statusTitle", (Object)(notFound ? "\u9875\u9762\u672a\u627e\u5230" : "\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528"));
        model.addAttribute("statusMessage", (Object)(notFound ? "\u8fd9\u4e2a\u5730\u5740\u6ca1\u6709\u5bf9\u5e94\u5185\u5bb9\uff0c\u53ef\u80fd\u5df2\u7ecf\u6539\u540d\u6216\u4e0d\u518d\u63d0\u4f9b\u3002\u4f60\u53ef\u4ee5\u4ece\u4e0b\u9762\u7684\u5165\u53e3\u7ee7\u7eed\u3002" : "\u670d\u52a1\u521a\u521a\u9047\u5230\u4e86\u4e00\u70b9\u95ee\u9898\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\uff0c\u6216\u76f4\u63a5\u8054\u7cfb\u5ba2\u670d\u5e2e\u4f60\u5904\u7406\u3002"));
        return "error";
    }

    private int statusCode(HttpServletRequest request) {
        Object raw = request.getAttribute("jakarta.servlet.error.status_code");
        if (raw == null) {
            return 500;
        }
        try {
            int code = Integer.parseInt(raw.toString());
            return code >= 400 && code <= 599 ? code : 500;
        }
        catch (NumberFormatException e) {
            return 500;
        }
    }
}

