package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.service.XingweiOrgRbacService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class XingweiWeComHrServiceTest {
    final ObjectMapper mapper=new ObjectMapper();
    XingweiWeComWriteClient client;XingweiOrgRbacService org;XingweiWeComHrService service;
    @BeforeEach void setup() throws Exception {
        client=mock(XingweiWeComWriteClient.class);org=mock(XingweiOrgRbacService.class);service=new XingweiWeComHrService(client,org,mapper);
        when(client.ready()).thenReturn(true);
        when(client.departments()).thenReturn(json("{\"department\":[{\"id\":1,\"parentid\":0,\"name\":\"企业\"},{\"id\":2,\"parentid\":1,\"name\":\"业务中心\"},{\"id\":3,\"parentid\":1,\"name\":\"运营中心\"}]}"));
        when(org.positions()).thenReturn(List.of(Map.of("code","P001","name","客服","departmentName","业务中心","enabled",true),Map.of("code","P002","name","运营","departmentName","运营中心","enabled",true)));
        when(org.departments()).thenReturn(List.of(Map.of("name","业务中心","enabled",true),Map.of("name","运营中心","enabled",true)));
        when(client.member("member-a")).thenReturn(json("{\"userid\":\"member-a\",\"name\":\"员工甲\",\"department\":[2],\"position\":\"客服\",\"status\":1,\"enable\":1}"));
    }
    JsonNode json(String text) throws Exception{return mapper.readTree(text);}
    XingweiWeComHrService.Command command(String action,String name,Long department,String position){return new XingweiWeComHrService.Command(action,"member-a",name,"13800000000","",department,position);}
    @Test void createValidatesCanonicalDepartmentAndPositionAndDoesNotWriteDuringPreview() throws Exception {
        var plan=service.prepare(command("CREATE_MEMBER","新员工",2L,"P001"));
        assertTrue(plan.targetId().matches("xh_[a-f0-9]{32}"));assertEquals("客服",plan.payload().path("position").asText());
        assertEquals(2,plan.payload().path("department").get(0).asInt());assertFalse(plan.toString().contains("13800000000"));
        service.validateCurrent(plan);verify(client,never()).createMember(any());
        assertThrows(ResponseStatusException.class,()->service.prepare(command("CREATE_MEMBER","员工",3L,"P001")));
        assertThrows(ResponseStatusException.class,()->service.prepare(command("CREATE_MEMBER","员工",999L,"P001")));
        when(client.createMember(any())).thenReturn(json("{\"errcode\":0}"));
        when(client.member(plan.targetId())).thenReturn(json("{\"userid\":\""+plan.targetId()+"\",\"name\":\"新员工\",\"department\":[2],\"position\":\"客服\",\"status\":4,\"enable\":1}"));
        service.execute(plan);verify(client,times(1)).createMember(any());
    }
    @Test void transferRejectsStaleMemberAndChangedPositionCatalogBeforeWriting() throws Exception {
        final var plan=service.prepare(command("TRANSFER_MEMBER","",3L,"P002"));
        when(client.member("member-a")).thenReturn(json("{\"userid\":\"member-a\",\"name\":\"已改名\",\"department\":[2],\"position\":\"客服\",\"status\":1,\"enable\":1}"));
        assertThrows(ResponseStatusException.class,()->service.validateCurrent(plan));verify(client,never()).updateMember(any());
        setup();var secondPlan=service.prepare(command("TRANSFER_MEMBER","",3L,"P002"));
        when(org.positions()).thenReturn(List.of());
        assertThrows(ResponseStatusException.class,()->service.validateCurrent(secondPlan));verify(client,never()).updateMember(any());
    }
    @Test void disableRequiresActualRemoteReadbackAndNeverRetriesUnconfirmedWrites() throws Exception {
        var plan=service.prepare(command("DISABLE_MEMBER","",null,null));service.validateCurrent(plan);
        assertThrows(ResponseStatusException.class,()->service.execute(plan));verify(client,times(1)).updateMember(any());
        when(client.member("member-a")).thenReturn(json("{\"userid\":\"member-a\",\"name\":\"员工甲\",\"status\":2,\"enable\":0}"));
        service.execute(plan);verify(client,times(2)).updateMember(argThat(body->body.path("enable").asInt()==0 && body.size()==2));
    }
    @Test void departmentCreateAndRenameValidateParentDuplicatesRootAndReadBackActualId() throws Exception {
        assertThrows(ResponseStatusException.class,()->service.prepare(command("CREATE_DEPARTMENT","业务中心",1L,null)));
        assertThrows(ResponseStatusException.class,()->service.prepare(command("RENAME_DEPARTMENT","根部门",1L,null)));
        var plan=service.prepare(command("CREATE_DEPARTMENT","新部门",1L,null));service.validateCurrent(plan);
        when(client.createDepartment(any())).thenReturn(json("{\"errcode\":0,\"id\":9}"));
        assertThrows(ResponseStatusException.class,()->service.execute(plan));
        when(client.departments()).thenReturn(json("{\"department\":[{\"id\":9,\"parentid\":1,\"name\":\"新部门\"}]}"));
        service.execute(plan);
    }
    @Test void disabledWriterNeverTouchesDirectoryAndPrivateCommandTextStaysRedacted() {
        when(client.ready()).thenReturn(false);
        var input=command("CREATE_MEMBER","员工",2L,"P001");assertFalse(input.toString().contains("13800000000"));
        assertThrows(ResponseStatusException.class,()->service.prepare(input));verify(client,never()).departments();
    }
}
