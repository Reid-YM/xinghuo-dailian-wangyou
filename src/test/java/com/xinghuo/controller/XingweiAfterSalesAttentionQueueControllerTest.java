package com.xinghuo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xinghuo.service.XingweiAfterSalesAttentionQueueService;
import com.xinghuo.service.XingweiAfterSalesAttentionQueueService.AttentionQueueItem;
import com.xinghuo.service.XingweiAfterSalesAttentionQueueService.AttentionQueueSnapshot;
import com.xinghuo.service.XingweiAfterSalesCaseService.CaseRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiAfterSalesAttentionQueueControllerTest {
    @Test
    void attentionQueuePublishesCompatibleShapePlusWaitingDuration() throws Exception {
        var service = mock(XingweiAfterSalesAttentionQueueService.class);
        var actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        var record = new CaseRecord(9L, "O9", "OTHER", "其他问题", "需要跟进",
                1000L, "OPEN", null, null, null, 0L);
        when(service.queue("OVER_72H", "OPEN", "OTHER", "PENDING", "XW9", "OLDEST_FIRST", 0, actor))
                .thenReturn(new AttentionQueueSnapshot(List.of(new AttentionQueueItem(
                        "XW9", record, 2000L, true, 2000L, 300000000L, "OVER_72H")),
                        1L, 0, 50, "OPEN", "OTHER", "PENDING", "XW9", false));

        MockMvcBuilders.standaloneSetup(new XingweiAfterSalesAttentionQueueController(service)).build()
                .perform(get("/api/admin/xingwei/business/after-sales/cases/attention-queue")
                        .param("age", "OVER_72H")
                        .param("status", "OPEN")
                        .param("category", "OTHER")
                        .param("followup", "PENDING")
                        .param("q", "XW9")
                        .param("sort", "OLDEST_FIRST")
                        .principal(actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].businessNo").value("XW9"))
                .andExpect(jsonPath("$.items[0].record.recordId").value(9))
                .andExpect(jsonPath("$.items[0].pendingCustomerFollowup").value(true))
                .andExpect(jsonPath("$.items[0].attentionAtMs").value(2000))
                .andExpect(jsonPath("$.items[0].waitingMs").value(300000000L))
                .andExpect(jsonPath("$.items[0].ageBucket").value("OVER_72H"));
    }
}
