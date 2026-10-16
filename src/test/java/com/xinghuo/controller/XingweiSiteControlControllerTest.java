package com.xinghuo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.xinghuo.service.XingweiSiteControlService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class XingweiSiteControlControllerTest {
    @Test
    void httpResponsesCarryRevisionAndDisableCachingAndPatchBindsExpectedRevision()throws Exception{
        var service=mock(XingweiSiteControlService.class);var actor=new UsernamePasswordAuthenticationToken("admin","",List.of());
        when(service.read("frontend",actor)).thenReturn(new XingweiSiteControlService.State(Map.of("themeRadius","18"),"revision1"));
        when(service.update(eq("frontend"),isNull(),anyMap(),eq("\"revision1\""),eq(actor),any())).thenReturn(new XingweiSiteControlService.State(Map.of("themeRadius","22"),"revision2"));
        var mvc=MockMvcBuilders.standaloneSetup(new XingweiSiteControlController(service)).build();
        mvc.perform(get("/api/admin/xingwei/site/frontend").principal(actor)).andExpect(status().isOk()).andExpect(header().string("ETag","\"revision1\"")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(patch("/api/admin/xingwei/site/frontend").principal(actor).header("If-Match","\"revision1\"").contentType(MediaType.APPLICATION_JSON).content("{\"themeRadius\":\"22\"}"))
            .andExpect(status().isOk()).andExpect(header().string("ETag","\"revision2\"")).andExpect(jsonPath("$.themeRadius").value("22"));
    }
}
