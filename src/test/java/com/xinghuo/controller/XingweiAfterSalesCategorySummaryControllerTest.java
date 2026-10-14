package com.xinghuo.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xinghuo.service.XingweiAfterSalesCategorySummaryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiAfterSalesCategorySummaryControllerTest {
    @Test
    void categorySummaryPublishesStableJsonFields() throws Exception {
        var categories = mock(XingweiAfterSalesCategorySummaryService.class);
        var actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(categories.summary(actor)).thenReturn(
                new XingweiAfterSalesCategorySummaryService.CategorySummary(4, 2, 1, 3));

        MockMvcBuilders.standaloneSetup(new XingweiAfterSalesCategorySummaryController(categories)).build()
                .perform(get("/api/admin/xingwei/business/after-sales/cases/category-summary").principal(actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryCount").value(4))
                .andExpect(jsonPath("$.cancellationCount").value(2))
                .andExpect(jsonPath("$.refundReviewCount").value(1))
                .andExpect(jsonPath("$.otherCount").value(3));
    }
}
