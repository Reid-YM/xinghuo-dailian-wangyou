package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiAuditQueryServiceTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard guard;
    private XingweiAuditQueryService service;
    private Authentication admin,dept,self;
    private final ObjectMapper mapper=new ObjectMapper();
    @BeforeEach
    void setup(){
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:audit"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT PRIMARY KEY,action VARCHAR(64),target_type VARCHAR(64),target_id VARCHAR(100),operator_name VARCHAR(100),reason VARCHAR(500),request_id VARCHAR(100),created_at TIMESTAMP,before_json TEXT,after_json TEXT)");
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(64))");
        jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT PRIMARY KEY,department_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_departments(id BIGINT PRIMARY KEY,code VARCHAR(16))");
        jdbc.update("INSERT INTO admin_users VALUES(1,'admin'),(2,'dept'),(3,'self'),(4,'peer'),(5,'other')");
        jdbc.update("INSERT INTO xw_departments VALUES(3,'03'),(4,'04')");
        jdbc.update("INSERT INTO xw_admin_profiles VALUES(2,3),(3,3),(4,3),(5,4)");
        guard=mock(XingweiPermissionGuard.class);
        admin=actor("admin",1L,"ALL","03");dept=actor("dept",2L,"DEPARTMENT","03");self=actor("self",3L,"SELF","03");
        service=new XingweiAuditQueryService(jdbc,guard,mapper);
    }
    @Test void memberActivityRequiresGlobalAuditScopeAndNeverSerializesRawPersonalData()throws Exception{
        jdbc.execute("CREATE TABLE customer_accounts(id BIGINT PRIMARY KEY,public_user_id VARCHAR(80),email VARCHAR(160))");
        jdbc.execute("CREATE TABLE user_audit_log(id BIGINT PRIMARY KEY,user_id BIGINT,action VARCHAR(40),detail VARCHAR(500),ip VARCHAR(45),user_agent VARCHAR(300),created_at TIMESTAMP)");
        jdbc.update("INSERT INTO customer_accounts VALUES(1,'MEM%1','PRIVATE_EMAIL@example.invalid')");
        for(int n=1;n<=51;n++)jdbc.update("INSERT INTO user_audit_log VALUES(?,1,'LOGIN_OK','DO-NOT-EXPOSE-DETAIL','192.168.10.23','PRIVATE-USER-AGENT',?)",n,Timestamp.from(Instant.parse("2026-10-15T16:00:00Z")));
        assertThrows(AccessDeniedException.class,()->service.memberActivity(dept,"","",null,null,0));
        assertThrows(AccessDeniedException.class,()->service.memberActivity(self,"","",null,null,0));
        var page=service.memberActivity(admin,"%","LOGIN_OK",LocalDate.of(2026,10,16),LocalDate.of(2026,10,16),0);
        assertEquals(51L,page.get("totalCount"));assertEquals(50,((List<?>)page.get("items")).size());
        assertEquals(1,((List<?>)service.memberActivity(admin,"","",null,null,1).get("items")).size());
        String serialized=mapper.writeValueAsString(page);
        assertTrue(serialized.contains("192.168.*.*"));assertTrue(serialized.contains("MEM%1"));
        for(String secret:List.of("PRIVATE_EMAIL","DO-NOT-EXPOSE","192.168.10.23","PRIVATE-USER-AGENT"))assertFalse(serialized.contains(secret));
        assertEquals(0L,service.memberActivity(admin,"","",LocalDate.of(2026,10,15),LocalDate.of(2026,10,15),0).get("totalCount"));
        assertThrows(IllegalArgumentException.class,()->service.memberActivity(admin,"","",null,null,-1));
        when(guard.has(admin,"audit:view")).thenReturn(false);
        assertThrows(AccessDeniedException.class,()->service.memberActivity(admin,"","",null,null,0));
    }
    private Authentication actor(String name,Long id,String scope,String department){
        Authentication a=new UsernamePasswordAuthenticationToken(name,"",List.of());
        when(guard.resolve(a)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),scope,id,department));
        when(guard.has(a,"audit:view")).thenReturn(true);return a;
    }
    private void add(long id,String operator,String action,String before,String after,String at){
        jdbc.update("INSERT INTO audit_logs VALUES(?,?,?,?,?,?,?,?,?,?)",id,action,"ORDER","ORDER-"+id,operator,"业务维护","REQ-"+id,Timestamp.from(Instant.parse(at)),before,after);
    }
    private Map<String,Object> list(Authentication a){return service.list(a,"","",null,null,0);}
    @Test
    void allDepartmentSelfAndMissingDepartmentScopesApplyToListAndDetail(){
        add(1,"dept","UPDATE",null,null,"2026-10-16T00:00:00Z");add(2,"self","UPDATE",null,null,"2026-10-16T00:00:00Z");
        add(3,"peer","UPDATE",null,null,"2026-10-16T00:00:00Z");add(4,"other","EXTERNAL",null,null,"2026-10-16T00:00:00Z");
        assertEquals(4L,list(admin).get("totalCount"));assertEquals(3L,list(dept).get("totalCount"));assertEquals(1L,list(self).get("totalCount"));
        assertFalse(((List<?>)list(dept).get("actions")).contains("EXTERNAL"));
        assertThrows(AccessDeniedException.class,()->service.detail(4,dept));assertThrows(AccessDeniedException.class,()->service.detail(3,self));
        when(guard.resolve(dept)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"DEPARTMENT",2L,null));
        assertEquals(1L,list(dept).get("totalCount"));
        when(guard.has(self,"audit:view")).thenReturn(false);assertThrows(AccessDeniedException.class,()->list(self));
    }
    @Test
    void rawSecretsNestedContentAndUnknownStatusNeverReachSerializedDetail()throws Exception{
        add(1,"self","UPDATE","{\"password\":\"SECRET_BEFORE\",\"status\":\"OPEN\",\"version\":1,\"nested\":{\"token\":\"TOKEN_BEFORE\"}}",
            "{\"password\":\"SECRET_AFTER\",\"status\":\"RESOLVED\",\"version\":2,\"nested\":{\"token\":\"TOKEN_AFTER\"}}","2026-10-16T00:00:00Z");
        String json=mapper.writeValueAsString(service.detail(1,self));
        assertFalse(json.contains("SECRET_"));assertFalse(json.contains("TOKEN_"));assertFalse(json.contains("beforeJson"));
        assertTrue(json.contains("OPEN"));assertTrue(json.contains("RESOLVED"));assertTrue(json.contains("内容已隐藏"));
        add(2,"self","UPDATE",null,"{\"status\":\"SECRET_UPPERCASE\"}","2026-10-16T00:00:00Z");
        assertFalse(mapper.writeValueAsString(service.detail(2,self)).contains("SECRET_UPPERCASE"));
    }
    @Test
    void internalNotesAndTechnicalChangesAreAbsentFromAuditSummaries() throws Exception {
        add(1,"self","UPDATE","{\"internalNote\":\"历史员工备注\",\"notes\":\"员工备注\",\"operatorNote\":\"内部反馈\",\"version\":1}",
                "{\"internalNote\":\"更新员工备注\",\"notes\":\"更新备注\",\"operatorNote\":\"更新内部反馈\",\"version\":2}","2026-10-16T00:00:00Z");
        var detail=service.detail(1,self);
        assertTrue(((java.util.List<?>)detail.get("changes")).isEmpty());
        String json=mapper.writeValueAsString(detail);
        assertFalse(json.contains("internalNote"));assertFalse(json.contains("requestId"));
        assertFalse(json.contains("targetId"));assertFalse(json.contains("reason"));
    }
    @Test
    void malformedJsonAndNonObjectContentStayHidden()throws Exception{
        add(1,"self","UPDATE","SECRET_MALFORMED","{broken","2026-10-16T00:00:00Z");
        add(2,"self","UPDATE",null,"[\"SECRET_ARRAY\"]","2026-10-16T00:00:00Z");
        assertFalse(mapper.writeValueAsString(service.detail(1,self)).contains("SECRET_MALFORMED"));
        assertFalse(mapper.writeValueAsString(service.detail(2,self)).contains("SECRET_ARRAY"));
    }
    @Test
    void dateFiltersUseBeijingCalendarDayAndSearchUsesLiteralWildcards(){
        add(1,"self","UPDATE",null,null,"2026-10-15T15:59:59Z");add(2,"self","UPDATE",null,null,"2026-10-15T16:00:00Z");
        assertEquals(1L,service.list(self,"","",LocalDate.of(2026,10,16),LocalDate.of(2026,10,16),0).get("totalCount"));
        jdbc.update("UPDATE audit_logs SET target_id='100%' WHERE id=2");
        assertEquals(1L,service.list(self,"%","",null,null,0).get("totalCount"));
        assertThrows(IllegalArgumentException.class,()->service.list(self,"","",LocalDate.of(2026,10,17),LocalDate.of(2026,10,16),0));
        assertThrows(IllegalArgumentException.class,()->service.list(self,"x".repeat(81),"",null,null,0));
    }
    @Test
    void paginationIsStableAndScoped(){
        for(int n=1;n<=51;n++)add(n,"self","UPDATE",null,null,"2026-10-16T00:00:00Z");
        add(52,"other","UPDATE",null,null,"2026-10-16T00:00:00Z");
        Map<String,Object> first=list(self),second=service.list(self,"","",null,null,1);
        assertEquals(51L,first.get("totalCount"));assertEquals(50,((List<?>)first.get("items")).size());
        assertEquals(1,((List<?>)second.get("items")).size());
        assertEquals(51L,((XingweiAuditQueryService.AuditItem)((List<?>)first.get("items")).getFirst()).id());
    }
}
