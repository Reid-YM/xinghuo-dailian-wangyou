package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicUtilitySeoFilterTest {

    private final PublicUtilitySeoFilter filter = new PublicUtilitySeoFilter();

    @Test
    void legacyOrderListPermanentlyRedirectsToRecords() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/order");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(301, response.getStatus());
        assertEquals("/records", response.getHeader("Location"));
        assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
    }

    @Test
    void legacyOrderDetailPermanentlyRedirectsToRecordDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/order/XH7KM4Q2AZ8P");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(301, response.getStatus());
        assertEquals("/record/XH7KM4Q2AZ8P", response.getHeader("Location"));
        assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
    }

    @Test
    void currentRecordUtilitiesRemainReachableButNoindex() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/records");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
    }
    @Test
    void historicalOrderAliasesKeepOnlyTheExplicitRollbackChoice() throws Exception {
        for (String path : java.util.List.of("/order", "/order/XHABCDEF1234")) {
            var request = new MockHttpServletRequest("GET", path);
            request.setParameter("legacy", "1"); request.setParameter("token", "private-test-value");
            var response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, new MockFilterChain());
            assertEquals(301, response.getStatus());
            assertEquals((path.equals("/order") ? "/records" : "/record/XHABCDEF1234") + "?legacy=1", response.getHeader("Location"));
        }
    }

    @Test void previewOrderAliasesStayInPreviewAndHistoricalServicesKeepTheirQuery() throws Exception {
        for(String base:java.util.List.of("","/next")){
            var old=new MockHttpServletRequest("GET",base+"/order/XHABCDEF1234");var response=new MockHttpServletResponse();
            filter.doFilterInternal(old,response,new MockFilterChain());assertEquals(base+"/record/XHABCDEF1234",response.getHeader("Location"));
            var redirects=new LegacyRedirectFilter(org.mockito.Mockito.mock(RouteHealthMonitor.class));
            for(var alias:java.util.Map.of("/loadouts","/mastery","/loadouts/","/mastery","/service/dmz-task","/service/mw4-dmz","/service/camo","/service/camo-task","/service/other","/service/other-task").entrySet()){
                var request=new MockHttpServletRequest("GET",base+alias.getKey());request.setQueryString("legacy=1&source=old-link");var result=new MockHttpServletResponse();
                redirects.doFilterInternal(request,result,new MockFilterChain());assertEquals(301,result.getStatus());assertEquals(base+alias.getValue()+"?legacy=1&source=old-link",result.getHeader("Location"));
            }
        }
    }


    @Test
    void enabledNextRecordsAreNoindexNofollowAndLegacyRollbackKeepsOldPolicy() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "frontendNextEnabled", true);
        for (String path : java.util.List.of("/records", "/record/XHABCDEF1234")) {
            var nextRequest = new MockHttpServletRequest("GET", path);
            var nextResponse = new MockHttpServletResponse();
            filter.doFilterInternal(nextRequest, nextResponse, new MockFilterChain());
            assertEquals("noindex,nofollow", nextResponse.getHeader("X-Robots-Tag"));

            var legacyRequest = new MockHttpServletRequest("GET", path);
            legacyRequest.setParameter("legacy", "1");
            var legacyResponse = new MockHttpServletResponse();
            filter.doFilterInternal(legacyRequest, legacyResponse, new MockFilterChain());
            assertEquals("noindex, follow", legacyResponse.getHeader("X-Robots-Tag"));
        }
    }

    @Test
    void explicitNextPreviewUsesPrivatePolicyEvenWhenDefaultIsLegacy() throws Exception {
        var request = new MockHttpServletRequest("GET", "/next/records");
        var response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());
        assertEquals("noindex,nofollow", response.getHeader("X-Robots-Tag"));
    }
}
