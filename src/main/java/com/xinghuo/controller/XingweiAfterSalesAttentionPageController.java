package com.xinghuo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Read-only V4K attention-aging queue page. /admin/** remains protected by the existing security chain. */
@Controller
public class XingweiAfterSalesAttentionPageController {
    @GetMapping("/admin/xingwei/after-sales/attention")
    String attentionQueue() {
        return "admin-xingwei-after-sales-attention";
    }
}
