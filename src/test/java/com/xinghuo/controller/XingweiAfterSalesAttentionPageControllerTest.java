package com.xinghuo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiAfterSalesAttentionPageControllerTest {
    @Test
    void attentionQueuePageUsesDedicatedTemplate() throws Exception {
        MockMvcBuilders.standaloneSetup(new XingweiAfterSalesAttentionPageController()).build()
                .perform(get("/admin/xingwei/after-sales/attention"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-xingwei-after-sales-attention"));
    }
}
