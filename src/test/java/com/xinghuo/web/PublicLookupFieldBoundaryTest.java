package com.xinghuo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.controller.PublicApiController;
import com.xinghuo.controller.PublicResponseSanitizerAdvice;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.service.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real lookup controller/services with repositories at the isolation boundary. */
class PublicLookupFieldBoundaryTest {
    static final String BUSINESS="XHABC1234567", LEGACY="XH-"+"A".repeat(24);
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    final CustomerOrderRepository orderRepo=mock(CustomerOrderRepository.class);
    final QuoteRepository quoteRepo=mock(QuoteRepository.class);
    final SiteConfigService config=mock(SiteConfigService.class);
    final UploadService uploads=mock(UploadService.class);
    CustomerOrder order; Quote quote; MockMvc mvc;
    @BeforeEach void setup(){
        UUID source=UUID.randomUUID(), image=UUID.randomUUID();
        order=CustomerOrder.builder().id(UUID.randomUUID()).orderNo(LEGACY).businessNo(BUSINESS)
                .sourceQuoteId(source).status(CustomerOrder.Status.IN_PROGRESS).progressPercent(45)
                .progressText("Public progress").customerNote("Customer-visible note")
                .internalNote("private-order-marker").version(17).progressImageAssetId(image)
                .createdAt(Instant.parse("2026-10-19T01:00:00Z")).updatedAt(Instant.parse("2026-10-19T02:00:00Z")).build();
        quote=Quote.builder().id(source).businessNo(BUSINESS).game("Actual Game").demand("Public Service")
                .status(Quote.Status.QUOTED).customerContact("private-contact-marker")
                .customerIpHash("private-ip-marker").followUpNote("private-followup-marker").build();
        when(orderRepo.findByBusinessNo(BUSINESS)).thenReturn(Optional.of(order));
        when(orderRepo.findByOrderNo(LEGACY)).thenReturn(Optional.of(order));
        when(quoteRepo.findById(source)).thenReturn(Optional.of(quote));
        when(quoteRepo.findByBusinessNo(BUSINESS)).thenReturn(Optional.of(quote));
        when(config.isOrderQueryEnabled()).thenReturn(true);
        var orders=new OrderService(orderRepo,uploads,mock(AuditService.class));
        var records=new RecordService(orderRepo,quoteRepo);
        var controller=new PublicApiController(config,null,null,orders,records,uploads,null,null,null,null,null,null,null);
        mvc=MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json), new org.springframework.http.converter.ResourceHttpMessageConverter())
                .setControllerAdvice(new PublicResponseSanitizerAdvice(json)).build();
    }
    @Test void businessAndHistoricalLookupsReturnOnlyPublicFieldsAndCannotBeCached() throws Exception {
        Set<String> recordFields=Set.of("businessNo","stage","status","title","expectedTime","progressPercent","progressText","createdAt","updatedAt","hasProgressImage","customerNote");
        Set<String> orderFields=Set.of("businessNo","status","progressPercent","progressText","updatedAt","hasProgressImage","customerNote");
        for(String route:List.of("records","orders")) for(String key:List.of(BUSINESS,LEGACY)){
            String body=mvc.perform(get("/api/"+route+"/"+key)).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$.businessNo").value(BUSINESS))
                    .andExpect(jsonPath("$.customerNote").value("Customer-visible note"))
                    .andExpect(jsonPath("$.progressPercent").value(45))
                    .andExpect(jsonPath("$.hasProgressImage").value(true)).andReturn().getResponse().getContentAsString();
            Set<String> fields=new HashSet<>();json.readTree(body).fieldNames().forEachRemaining(fields::add);
            assertEquals(route.equals("records")?recordFields:orderFields,fields);
            for(String secret:List.of("private-order-marker","private-contact-marker","private-ip-marker","private-followup-marker",order.getId().toString(),quote.getId().toString()))assertFalse(body.contains(secret));
        }
    }
    @Test void quoteFallbackKeepsPublicProgressWithoutExposingCrmNotes() throws Exception {
        when(orderRepo.findByBusinessNo(BUSINESS)).thenReturn(Optional.empty());
        var result=mvc.perform(get("/api/records/"+BUSINESS)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.stage").value("QUOTE"))
                .andExpect(jsonPath("$.title").value("Actual Game · Public Service")).andReturn().getResponse().getContentAsString();
        assertFalse(result.contains("private-"));assertFalse(result.contains(quote.getId().toString()));
    }
    @Test void progressImagesRemainBinaryAndRespectQuerySwitch() throws Exception {
        byte[] image={1,2,3,4};
        when(uploads.load(order.getProgressImageAssetId(),"ORDER",LEGACY)).thenReturn(new ByteArrayResource(image));
        when(uploads.mediaType(order.getProgressImageAssetId())).thenReturn(MediaType.IMAGE_PNG);
        for(String route:List.of("records","orders"))mvc.perform(get("/api/"+route+"/"+BUSINESS+"/image"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(image));
        when(config.isOrderQueryEnabled()).thenReturn(false);
        for(String route:List.of("records","orders")){
            mvc.perform(get("/api/"+route+"/"+BUSINESS)).andExpect(status().isBadRequest());
            mvc.perform(get("/api/"+route+"/"+BUSINESS+"/image")).andExpect(status().isBadRequest());
        }
    }
}
