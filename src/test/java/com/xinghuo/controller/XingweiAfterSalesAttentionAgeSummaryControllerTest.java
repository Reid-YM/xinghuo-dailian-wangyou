package com.xinghuo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xinghuo.service.XingweiAfterSalesAttentionAgeSummaryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiAfterSalesAttentionAgeSummaryControllerTest {
    @Test
    void attentionAgeSummaryPublishesStableJsonFields() throws Exception {
        var aging = mock(XingweiAfterSalesAttentionAgeSummaryService.class);
        var actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(aging.summary(actor)).thenReturn(
                new XingweiAfterSalesAttentionAgeSummaryService.AttentionAgeSummary(9, 4, 3, 2));

        MockMvcBuilders.standaloneSetup(new XingweiAfterSalesAttentionAgeSummaryController(aging)).build()
                .perform(get("/api/admin/xingwei/business/after-sales/cases/attention-age-summary").principal(actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attentionCount").value(9))
                .andExpect(jsonPath("$.within24hCount").value(4))
                .andExpect(jsonPath("$.from24To72hCount").value(3))
                .andExpect(jsonPath("$.over72hCount").value(2));
    }
}
