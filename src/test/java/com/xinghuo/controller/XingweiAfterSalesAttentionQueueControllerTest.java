package com.xinghuo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xinghuo.service.XingweiAfterSalesAttentionQueueService;
import com.xinghuo.service.XingweiAfterSalesCaseService.CaseRecord;
import com.xinghuo.service.XingweiAfterSalesCaseService.QueueItem;
import com.xinghuo.service.XingweiAfterSalesCaseService.QueueSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiAfterSalesAttentionQueueControllerTest {
    @Test
    void attentionQueuePublishesExistingQueueShape() throws Exception {
        var service = mock(XingweiAfterSalesAttentionQueueService.class);
        var actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        var record = new CaseRecord(9L, "O9", "OTHER", "其他问题", "需要跟进",
                1000L, "OPEN", null, null, null, 0L);
        when(service.queue("OVER_72H", "OPEN", "OTHER", "PENDING", "XW9", 0, actor))
                .thenReturn(new QueueSnapshot(List.of(new QueueItem("XW9", record, 2000L, true)),
                        1L, 0, 50, "OPEN", "OTHER", "PENDING", "XW9", false));

        MockMvcBuilders.standaloneSetup(new XingweiAfterSalesAttentionQueueController(service)).build()
                .perform(get("/api/admin/xingwei/business/after-sales/cases/attention-queue")
                        .param("age", "OVER_72H")
                        .param("status", "OPEN")
                        .param("category", "OTHER")
                        .param("followup", "PENDING")
                        .param("q", "XW9")
                        .principal(actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].businessNo").value("XW9"))
                .andExpect(jsonPath("$.items[0].record.recordId").value(9))
                .andExpect(jsonPath("$.items[0].pendingCustomerFollowup").value(true));
    }
}
