package com.xinghuo.guide;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.service.AuditService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicGuideSearchTest {
    JdbcTemplate jdbc;
    GuideContentService service;

    @BeforeEach void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:public_guides_" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE game_catalog(code VARCHAR PRIMARY KEY,name VARCHAR,name_en VARCHAR)");
        jdbc.execute("CREATE TABLE guide_sections(guide_id BIGINT)");
        jdbc.execute("CREATE TABLE guide_products(guide_id BIGINT)");
        jdbc.execute("""
            CREATE TABLE game_guides(id BIGINT PRIMARY KEY,game_code VARCHAR,slug VARCHAR,title VARCHAR,
              subtitle VARCHAR,summary VARCHAR,cover_image VARCHAR,difficulty VARCHAR,estimated_minutes INT,
              language VARCHAR,author_name VARCHAR,source_name VARCHAR,source_url VARCHAR,status VARCHAR,
              is_featured INT,sort_order INT,tags_json VARCHAR,seo_title VARCHAR,seo_description VARCHAR,
              canonical_url VARCHAR,view_count BIGINT,published_at TIMESTAMP,updated_at TIMESTAMP)
            """);
        jdbc.update("INSERT INTO game_catalog VALUES('COMMON','普通游戏','Common Game'),('RARE','稀有游戏','Rare English Game')");
        for (int i=1;i<=209;i++) {
            String status=i==206?"DRAFT":i==208?"ARCHIVED":"PUBLISHED";
            Instant published=i==207?Instant.now().plusSeconds(86400):Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i);
            jdbc.update("""
                INSERT INTO game_guides(id,game_code,slug,title,subtitle,summary,status,is_featured,sort_order,
                  tags_json,view_count,published_at,updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,i,i>=205?"RARE":"COMMON","guide-"+i,i>=205?"冷门旧攻略":"普通攻略"+i,
                i>=205?"独有副标题":"", "攻略说明",status,i==1?1:0,i,
                i>=205?"[\"稀有标签\"]":"[]",i==205?9999:i,i==209?null:Timestamp.from(published),Timestamp.from(published));
        }
        service = new GuideContentService(jdbc,new ObjectMapper(),mock(AuditService.class));
    }

    @Test void searchRunsBeforeTheLimitAndPreservesEveryPublicVisibilityGate() {
        assertEquals(200,service.publicList(null,null,999).size());
        assertFalse(service.publicList(null,null,100).stream().anyMatch(g->g.id()==205));
        for(String q:List.of("冷门旧攻略","独有副标题","稀有标签","Rare English","稀有游戏","RARE")) {
            assertEquals(List.of(205L),service.publicList(null,q,200).stream().map(GuideModels.GuideSummary::id).toList(),q);
        }
        assertEquals(List.of(205L),service.publicList(" rare ",null,200).stream().map(GuideModels.GuideSummary::id).toList());
        assertTrue(service.publicList(null,"' OR 1=1 --",200).isEmpty());
        assertTrue(service.publicList("RARE' OR 1=1 --",null,200).isEmpty());
    }

    @Test void allSortModesApplyBeforeLimitAndUntrustedSortFallsBackToFeatured() {
        assertEquals(1L,service.publicList(null,null,1).getFirst().id());
        assertEquals(205L,service.publicList(null,null,1,"latest").getFirst().id());
        assertEquals(205L,service.publicList(null,null,1,"popular").getFirst().id());
        assertEquals(1L,service.publicList(null,null,1,"view_count; DROP TABLE game_guides").getFirst().id());
        assertEquals(1,service.publicList(null,null,0).size());
    }

    @Test void publicHttpApiAcceptsSearchGameAndSortWithoutChangingItsArrayResponse() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new PublicGuideController(service)).build();
        mvc.perform(get("/api/guides").param("q","Rare English").param("game","RARE").param("sort","popular").param("limit","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].slug").value("guide-205"));
        mvc.perform(get("/api/guides").param("limit","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].slug").value("guide-1"));
    }
}
