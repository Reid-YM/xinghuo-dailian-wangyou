package com.xinghuo.guide;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiGuideWorkspaceServiceTest {
    private XingweiGuideWorkspaceService service;
    private GuideContentService guides;
    private XingweiPermissionGuard guard;
    private JdbcTemplate jdbc;
    private Authentication actor;
    private GuideModels.GuideDetail detail;
    @BeforeEach
    void setup(){
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:guide_workspace"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE game_guides(id BIGINT PRIMARY KEY,version BIGINT NOT NULL,view_count BIGINT)");jdbc.update("INSERT INTO game_guides VALUES(1,3,0)");
        guard=mock(XingweiPermissionGuard.class);guides=mock(GuideContentService.class);actor=new UsernamePasswordAuthenticationToken("editor","",List.of());
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"ALL",1L,"03"));when(guard.has(actor,"content:view")).thenReturn(true);when(guard.has(actor,"content:manage")).thenReturn(true);
        detail=new ObjectMapper().convertValue(Map.of("guide",Map.of("id",1,"title","攻略草稿","status","DRAFT","slug","draft-guide"),"sections",List.of(),"products",List.of(),"faqs",List.of(),"relatedGuides",List.of()),GuideModels.GuideDetail.class);
        when(guides.adminDetail(1L)).thenReturn(detail);service=new XingweiGuideWorkspaceService(jdbc,guard,guides);
    }
    @Test
    void readAndGlobalWritingAreSeparatedAndDepartmentEditorCannotChangeGlobalContent(){
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"DEPARTMENT",1L,"03"));
        assertFalse((Boolean)service.access(actor).get("canManage"));assertEquals(detail,service.read(1,actor).detail());
        assertThrows(AccessDeniedException.class,()->service.save(1L,null,"guide-1-v3",actor,null));verify(guides,never()).update(any(),any(),any(),any());
        when(guard.has(actor,"content:view")).thenReturn(false);assertThrows(AccessDeniedException.class,()->service.read(1,actor));
    }
    @Test
    void existingVersionColumnPreventsStaleEditAndArchive(){
        assertEquals("guide-1-v3",service.read(1,actor).revision());
        jdbc.update("UPDATE game_guides SET version=4 WHERE id=1");
        assertThrows(OptimisticLockingFailureException.class,()->service.save(1L,null,"\"guide-1-v3\"",actor,null));
        assertThrows(OptimisticLockingFailureException.class,()->service.archive(1,"\"guide-1-v3\"",actor,null));
        verify(guides,never()).update(any(),any(),any(),any());verify(guides,never()).archive(any(),any(),any(),any());
    }
    @Test
    void saveReturnsNewVersionAndPublicViewCountDoesNotConflict(){
        jdbc.update("UPDATE game_guides SET view_count=100 WHERE id=1");
        when(guides.update(eq(1L),isNull(),eq("editor"),isNull())).thenAnswer(i->{jdbc.update("UPDATE game_guides SET version=4 WHERE id=1");return detail;});
        assertEquals("guide-1-v4",service.save(1L,null,"\"guide-1-v3\"",actor,null).revision());
    }
    @Test
    void bulkAndImportOperationsStillRequireGlobalContentManagement(){
        when(guard.has(actor,"content:manage")).thenReturn(false);
        assertThrows(AccessDeniedException.class,()->service.batch(null,actor,null));assertThrows(AccessDeniedException.class,()->service.archiveAll(null,actor,null));assertThrows(AccessDeniedException.class,()->service.importGames(null,actor,null));
        verifyNoInteractions(guides);
    }
}
