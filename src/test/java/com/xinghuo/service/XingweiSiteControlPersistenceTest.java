package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiSiteControlPersistenceTest {
    private JdbcTemplate jdbc;
    private XingweiSiteControlService service;
    private FrontendControlService frontend;
    private SiteSettingService settings;
    private AuditService audit;
    private Authentication actor;
    @BeforeEach
    void setup(){
        var ds=new DriverManagerDataSource("jdbc:h2:mem:site_persist"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);jdbc.execute("CREATE TABLE site_config(id BIGINT PRIMARY KEY)");jdbc.update("INSERT INTO site_config VALUES(1)");
        jdbc.execute("CREATE TABLE game_catalog(code VARCHAR(32) PRIMARY KEY,name VARCHAR(80))");jdbc.update("INSERT INTO game_catalog VALUES('MW4','实际游戏')");
        jdbc.execute("CREATE TABLE site_settings(setting_key VARCHAR(100) PRIMARY KEY,setting_value VARCHAR(2000))");
        var repo=mock(SiteSettingRepository.class);
        when(repo.findAll()).thenAnswer(i->jdbc.query("SELECT * FROM site_settings",(rs,n)->SiteSetting.builder().settingKey(rs.getString(1)).settingValue(rs.getString(2)).updatedAt(Instant.now()).build()));
        when(repo.findById(anyString())).thenAnswer(i->jdbc.query("SELECT * FROM site_settings WHERE setting_key=?",(rs,n)->SiteSetting.builder().settingKey(rs.getString(1)).settingValue(rs.getString(2)).build(),i.getArgument(0,String.class)).stream().findFirst());
        when(repo.save(any(SiteSetting.class))).thenAnswer(i->{SiteSetting row=i.getArgument(0);jdbc.update("MERGE INTO site_settings KEY(setting_key) VALUES(?,?)",row.getSettingKey(),row.getSettingValue());return row;});
        frontend=new FrontendControlService(repo);settings=new SiteSettingService(repo);audit=mock(AuditService.class);
        var guard=mock(XingweiPermissionGuard.class);actor=new UsernamePasswordAuthenticationToken("admin","",List.of());
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("LEGACY_ADMIN_OVERRIDE",true,List.of(),List.of(),"ALL",1L,null));
        when(guard.has(eq(actor),anyString())).thenReturn(true);
        var target=new XingweiSiteControlService(jdbc,guard,mock(SiteConfigService.class),settings,frontend,mock(GameStatusService.class),audit,new ObjectMapper(),mock(UploadService.class));
        var advice=new TransactionInterceptor();advice.setTransactionManager(new DataSourceTransactionManager(ds));advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy=new ProxyFactory(target);proxy.addAdvice(advice);service=(XingweiSiteControlService)proxy.getProxy();
    }
    @Test
    void newConsoleWritesReachExistingPublicConfigurationSources(){
        service.update("frontend",null,Map.of("themeAccentColor","#112233"),service.read("frontend",actor).revision(),actor,null);
        assertEquals("#112233",frontend.settings().get("themeAccentColor"));
        service.update("global-ui",null,Map.of("navServicesLabel","星火服务大厅"),service.read("global-ui",actor).revision(),actor,null);
        assertEquals("星火服务大厅",settings.publicSettings().get("navServicesLabel"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM site_settings",Integer.class));
    }
    @Test
    void auditFailureRollsBackPersistedSettings(){
        doThrow(new IllegalStateException("audit unavailable")).when(audit).log(any(),any(),any(),any(),any(),any(),any(),any());
        assertThrows(IllegalStateException.class,()->service.update("frontend",null,Map.of("themeAccentColor","#112233"),service.read("frontend",actor).revision(),actor,null));
        assertEquals("#f4bd34",frontend.settings().get("themeAccentColor"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM site_settings",Integer.class));
    }
    @Test
    void validationFailureRollsBackEarlierFieldsInTheSameRequest(){
        Map<String,Object> values=new LinkedHashMap<>();values.put("themeAccentColor","#112233");values.put("themeRadius","999");
        assertThrows(com.xinghuo.exception.BadRequestException.class,()->service.update("frontend",null,values,service.read("frontend",actor).revision(),actor,null));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM site_settings",Integer.class));
    }
}
