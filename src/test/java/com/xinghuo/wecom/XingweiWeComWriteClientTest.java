package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

class XingweiWeComWriteClientTest {
    HttpServer server; MockEnvironment environment; XingweiWeComWriteClient client;
    final ObjectMapper mapper=new ObjectMapper();final List<String> calls=new ArrayList<>();
    String writeResponse="{\"errcode\":0}";int writeStatus=200;AtomicInteger tokenCalls=new AtomicInteger();
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/cgi-bin",exchange->{
            calls.add(exchange.getRequestMethod()+" "+exchange.getRequestURI().getPath());
            String response;
            if(exchange.getRequestURI().getPath().endsWith("gettoken")){
                tokenCalls.incrementAndGet();
                assertTrue(exchange.getRequestURI().getRawQuery().contains("corpsecret=write-only-test"));
                assertFalse(exchange.getRequestURI().getRawQuery().contains("read-only-test"));
                response="{\"errcode\":0,\"access_token\":\"private-write-token\",\"expires_in\":7200}";
            }else if(exchange.getRequestMethod().equals("POST")){
                assertTrue(exchange.getRequestURI().getRawQuery().contains("access_token=private-write-token"));
                var body=mapper.readTree(exchange.getRequestBody());assertEquals("member-a",body.path("userid").asText());
                response=writeResponse;
            }else response="{\"errcode\":0,\"department\":[]}";
            byte[] data=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(exchange.getRequestMethod().equals("POST")?writeStatus:200,data.length);
            exchange.getResponseBody().write(data);exchange.close();
        });server.start();
        environment=new MockEnvironment().withProperty("wecom.corp-id","test-corp").withProperty("wecom.app-secret","read-only-test").withProperty("wecom.write-secret","write-only-test").withProperty("wecom.hr-write-enabled","true");
        client=new XingweiWeComWriteClient(environment,mapper,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/cgi-bin"));
    }
    @AfterEach void close(){server.stop(0);}
    @Test void dedicatedTokenIsCachedAndActualPostUsesOnlyWriteCredential() {
        client.departments();client.updateMember(mapper.createObjectNode().put("userid","member-a").put("enable",0));
        assertEquals(1,tokenCalls.get());assertEquals(List.of("GET /cgi-bin/gettoken","GET /cgi-bin/department/list","POST /cgi-bin/user/update"),calls);
    }
    @Test void disabledFlagMakesZeroNetworkRequestsEvenWithPreparedCredentials() {
        environment.withProperty("wecom.hr-write-enabled","false");assertFalse(client.ready());
        assertThrows(ResponseStatusException.class,()->client.updateMember(mapper.createObjectNode().put("userid","member-a")));
        assertTrue(calls.isEmpty());
    }
    @Test void failedPostIsNeverRetriedAndVendorDiagnosticsCannotEscape() {
        writeResponse="{\"errcode\":40014,\"errmsg\":\"private-write-token write-only-test mobile=13800000000\"}";
        var exception=assertThrows(ResponseStatusException.class,()->client.updateMember(mapper.createObjectNode().put("userid","member-a")));
        assertFalse(exception.toString().contains("private-write-token"));assertFalse(exception.toString().contains("write-only-test"));
        assertNull(exception.getCause());assertEquals(2,calls.size());assertEquals(1,tokenCalls.get());
    }
    @Test void nonJsonAndHttpFailureAlsoProduceSafeUncertainResultWithoutRetry() {
        writeResponse="upstream private-write-token";writeStatus=502;
        var exception=assertThrows(ResponseStatusException.class,()->client.updateMember(mapper.createObjectNode().put("userid","member-a")));
        assertFalse(exception.toString().contains("private-write-token"));assertEquals(2,calls.size());
    }
}
