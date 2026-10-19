package com.xinghuo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.controller.XingweiResponseSanitizerAdvice;
import com.xinghuo.service.XingweiAuditQueryService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class XingweiInternalFieldsBoundaryTest {
    @RestController static class Probe {
        final Map<String,Object> order=Map.of("id","opaque-order-key","version",7,
                "internalNote","历史员工备注","customerNote","客户可见说明");
        @GetMapping("/api/admin/xingwei/business/orders") Object orders() {
            return Map.of("items",List.of(order),"case",Map.of("recordId",11,"version",3,
                    "handlingNote","内部处理说明","customerReply","正在核对"),"feedback",Map.of("operatorNote","内部反馈备注","followUpNote","内部跟进记录"),
                    "channel",Map.of("channelCode","TAOBAO","serviceState","AVAILABLE","note","内部渠道备注"));
        }
        @GetMapping("/api/admin/xingwei/audits") Object audits() {
            return Map.of("items",List.of(new XingweiAuditQueryService.AuditItem(9,"UPDATE","ORDER",
                    "database-key","staff","历史内部处理说明","request-key","2026-10-19")));
        }
        @GetMapping("/api/elsewhere") Object elsewhere() { return order; }
    }
    @Test void nativeResponsesHideNestedNotesButKeepBusinessTextAndConcurrencyProtocol() throws Exception {
        Probe controller=new Probe();
        var mvc=MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new XingweiResponseSanitizerAdvice(new ObjectMapper())).build();
        mvc.perform(get("/api/admin/xingwei/business/orders"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].internalNote").doesNotExist())
                .andExpect(jsonPath("$.case.handlingNote").doesNotExist())
                .andExpect(jsonPath("$.feedback.operatorNote").doesNotExist())
                .andExpect(jsonPath("$.feedback.followUpNote").doesNotExist())
                .andExpect(jsonPath("$.channel.note").doesNotExist())
                .andExpect(jsonPath("$.items[0].customerNote").value("客户可见说明"))
                .andExpect(jsonPath("$.case.customerReply").value("正在核对"))
                .andExpect(jsonPath("$.items[0].id").value("opaque-order-key"))
                .andExpect(jsonPath("$.items[0].version").value(7))
                .andExpect(jsonPath("$.case.recordId").value(11));
        assertEquals("历史员工备注",controller.order.get("internalNote"));
        mvc.perform(get("/api/elsewhere")).andExpect(jsonPath("$.internalNote").value("历史员工备注"));
    }
    @Test void auditSummariesDoNotReturnInternalRemarksOrTechnicalLookupFields() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new Probe())
                .setControllerAdvice(new XingweiResponseSanitizerAdvice(new ObjectMapper())).build();
        mvc.perform(get("/api/admin/xingwei/audits"))
                .andExpect(jsonPath("$.items[0].reason").doesNotExist())
                .andExpect(jsonPath("$.items[0].targetId").doesNotExist())
                .andExpect(jsonPath("$.items[0].requestId").doesNotExist())
                .andExpect(jsonPath("$.items[0].id").value(9))
                .andExpect(jsonPath("$.items[0].operatorName").value("staff"));
    }
}
