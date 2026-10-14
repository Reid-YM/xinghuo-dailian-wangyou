package com.xinghuo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xinghuo.service.XingweiAfterSalesCaseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiAfterSalesQueueControllerTest {
    @Test
    void summaryKeepsExistingFieldsAndPublishesStatusDistribution() throws Exception {
        var cases = mock(XingweiAfterSalesCaseService.class);
        var actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(cases.summary(actor)).thenReturn(
                new XingweiAfterSalesCaseService.CaseSummary(7, 2, 3, 1, 3));

        MockMvcBuilders.standaloneSetup(new XingweiAfterSalesQueueController(cases)).build()
                .perform(get("/api/admin/xingwei/business/after-sales/cases/summary").principal(actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(7))
                .andExpect(jsonPath("$.pendingFollowupCount").value(2))
                .andExpect(jsonPath("$.openCount").value(3))
                .andExpect(jsonPath("$.inProgressCount").value(1))
                .andExpect(jsonPath("$.resolvedCount").value(3));
    }
}
