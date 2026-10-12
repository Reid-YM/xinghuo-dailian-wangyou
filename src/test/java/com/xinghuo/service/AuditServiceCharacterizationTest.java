package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

class AuditServiceCharacterizationTest {

    @Test
    void logPersistsTrustedClientIpRequestIdAndSerializedSnapshots() throws Exception {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        Object before = new Object();
        Object after = new Object();
        when(mapper.writeValueAsString(before)).thenReturn("{\"before\":1}");
        when(mapper.writeValueAsString(after)).thenReturn("{\"after\":2}");
        AuditService service = new AuditService(repo, mapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.77");
        request.setAttribute("requestId", "req-attr-123");
        request.addHeader("X-Request-Id", "req-header-ignored");

        service.log("SERVICE_UPDATE", "SERVICE", "42", "admin", before, after, "调整价格", request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("SERVICE_UPDATE", saved.getAction());
        assertEquals("SERVICE", saved.getTargetType());
        assertEquals("42", saved.getTargetId());
        assertEquals("admin", saved.getOperatorName());
        assertEquals("{\"before\":1}", saved.getBeforeJson());
        assertEquals("{\"after\":2}", saved.getAfterJson());
        assertEquals("调整价格", saved.getReason());
        assertEquals("203.0.113.77", saved.getIpAddress());
        assertEquals("req-attr-123", saved.getRequestId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void requestIdFallsBackToHeaderAndNullRequestKeepsMetadataNull() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        ObjectMapper mapper = new ObjectMapper();
        AuditService service = new AuditService(repo, mapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        request.addHeader("X-Request-Id", "req-header-456");

        service.log("ONE", "TYPE", "1", "admin", null, null, null, request);
        service.log("TWO", "TYPE", "2", "admin", null, null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo, org.mockito.Mockito.times(2)).save(captor.capture());
        AuditLog withRequest = captor.getAllValues().get(0);
        AuditLog withoutRequest = captor.getAllValues().get(1);
        assertEquals("req-header-456", withRequest.getRequestId());
        assertEquals("192.0.2.20", withRequest.getIpAddress());
        assertNull(withoutRequest.getRequestId());
        assertNull(withoutRequest.getIpAddress());
    }

    @Test
    void jsonSerializationFailureFallsBackToStringValue() throws Exception {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        Object before = new Object() {
            @Override
            public String toString() {
                return "fallback-before";
            }
        };
        when(mapper.writeValueAsString(before)).thenThrow(new JsonProcessingException("boom") {});
        AuditService service = new AuditService(repo, mapper);

        service.log("TEST", "TYPE", "9", "admin", before, null, "reason", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        assertEquals("fallback-before", captor.getValue().getBeforeJson());
        assertNull(captor.getValue().getAfterJson());
        verify(mapper).writeValueAsString(before);
    }
}
