package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiSiteControlServiceTest {
    private XingweiSiteControlService service;
    private XingweiPermissionGuard guard;
    private SiteConfigService config;
    private SiteSettingService settings;
    private FrontendControlService frontend;
    private GameStatusService games;
    private AuditService audit;
    private UploadService uploads;
    private Authentication actor;
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    @BeforeEach
    void setup(){
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:site"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE site_config(id BIGINT PRIMARY KEY)");jdbc.update("INSERT INTO site_config VALUES(1)");
        guard=mock(XingweiPermissionGuard.class);config=mock(SiteConfigService.class);settings=mock(SiteSettingService.class);frontend=mock(FrontendControlService.class);games=mock(GameStatusService.class);audit=mock(AuditService.class);
        actor=new UsernamePasswordAuthenticationToken("manager","",List.of());
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"ALL",1L,"03"));
        when(guard.has(actor,"site:manage")).thenReturn(true);when(guard.has(actor,"site:view")).thenReturn(true);when(guard.hasAny(actor,"site:view","seo:view")).thenReturn(true);
        when(config.publicConfig()).thenReturn(mapper.convertValue(Map.of("brandName","星火","wechatQrImage","/api/site-assets/old","contactTitle","客服","heroTitle","原主标题"),ApiDtos.SiteConfigResponse.class));
        when(settings.globalUiSettings()).thenReturn(Map.of("navServicesLabel","服务","navServicesVisible","true"));
        when(frontend.settings()).thenReturn(Map.of("themeAccentColor","#f4bd34"));when(games.all()).thenReturn(List.of());
        uploads=mock(UploadService.class);service=new XingweiSiteControlService(jdbc,guard,config,settings,frontend,games,audit,mapper,uploads);
    }
    @Test
    void onlyAllScopeManagersCanChangeGlobalSettingsAndSeoUsesSeparatePermission(){
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"DEPARTMENT",1L,"03"));
        assertThrows(AccessDeniedException.class,()->service.update("global-ui",null,Map.of(),"",actor,null));
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"ALL",1L,"03"));
        assertThrows(AccessDeniedException.class,()->service.update("site","seo",Map.of("seoTitle","标题"),service.read("site",actor).revision(),actor,null));
        verify(config,never()).updateSection(any(),any(),any(),any());
    }
    @Test
    void etagRejectsStaleSavesAndDoesNotDependOnMapInsertionOrder(){
        var current=new AtomicReference<Map<String,String>>(Map.of("navServicesLabel","服务","navServicesVisible","true"));
        when(settings.globalUiSettings()).thenAnswer(i->current.get());
        when(settings.updateGlobalUiSettings(anyMap())).thenAnswer(i->{current.set(Map.of("navServicesLabel","新版服务","navServicesVisible","true"));return current.get();});
        String revision=service.read("global-ui",actor).revision();
        service.update("global-ui",null,Map.of("navServicesLabel","新版服务"),'"'+revision+'"',actor,null);
        assertThrows(OptimisticLockingFailureException.class,()->service.update("global-ui",null,Map.of("navServicesLabel","过期覆盖"),revision,actor,null));
        verify(settings,times(1)).updateGlobalUiSettings(anyMap());verify(audit,times(1)).log(any(),any(),any(),any(),any(),any(),any(),any());
        LinkedHashMap<String,String> reverse=new LinkedHashMap<>();reverse.put("navServicesVisible","true");reverse.put("navServicesLabel","新版服务");
        String first=service.read("global-ui",actor).revision();current.set(reverse);assertEquals(first,service.read("global-ui",actor).revision());
    }
    @Test
    @SuppressWarnings({"rawtypes","unchecked"})
    void sectionPatchPreservesUnsubmittedFieldsAndRejectsOtherSections(){
        String revision=service.read("site",actor).revision();
        service.update("site","contact",Map.of("contactTitle","新的客服"),revision,actor,null);
        ArgumentCaptor<Map> capture=ArgumentCaptor.forClass(Map.class);verify(config).updateSection(eq("contact"),capture.capture(),eq("manager"),isNull());
        assertEquals("/api/site-assets/old",capture.getValue().get("wechatQrImage"));
        assertEquals("新的客服",capture.getValue().get("contactTitle"));
        assertThrows(IllegalArgumentException.class,()->service.update("site","home",Map.of("wechatId","非法跨区"),revision,actor,null));
    }
    @Test
    void unsupportedFieldsAndExecutableUrlsAreRejectedBeforeServiceWrites(){
        String revision=service.read("site",actor).revision();
        for(String bad:List.of("javascript:alert(1)","//evil.test/x","/\\evil.test/x","https://user:pass@evil.test/x"))
            assertThrows(IllegalArgumentException.class,()->service.update("site","visual",Map.of("heroImage",bad),revision,actor,null));
        assertThrows(IllegalArgumentException.class,()->service.update("frontend",null,Map.of("unknownKey","bad"),service.read("frontend",actor).revision(),actor,null));
        verify(config,never()).updateSection(any(),any(),any(),any());verify(frontend,never()).update(any());
    }
    @Test
    void gameStateUpdatesUseRealGameStatusServiceRatherThanSiteFallback(){
        String revision=service.read("games",actor).revision();
        service.update("games","CODMW4",Map.of("statusCode","PAUSED","publicLabel","暂停接单","publicDetail","维护","ctaLabel","联系咨询"),revision,actor,null);
        verify(games).update("CODMW4","PAUSED","暂停接单","维护","联系咨询","manager",null,null);
        verify(config,never()).updateSection(any(),any(),any(),any());
    }
    @Test
    void uploadedImagesStayDraftUntilASectionIsSaved(){
        var asset=mock(com.xinghuo.model.UploadAsset.class);var id=java.util.UUID.randomUUID();when(asset.getId()).thenReturn(id);
        var file=mock(org.springframework.web.multipart.MultipartFile.class);when(uploads.storeImage(file,com.xinghuo.model.UploadAsset.Kind.SITE_HERO)).thenReturn(asset);
        assertEquals("/api/site-assets/"+id,service.upload("heroImage",file,actor,null).get("url"));
        verify(config,never()).updateSiteImage(any(),any(),any(),any());
        verify(uploads).bind(id,"SITE_CONFIG","heroImage");
        when(guard.has(actor,"site:manage")).thenReturn(false);
        assertThrows(AccessDeniedException.class,()->service.upload("heroImage",file,actor,null));
    }
    @Test
    void readOnlyAndDeniedActorsCannotBypassServiceAuthorization(){
        when(guard.has(actor,"site:manage")).thenReturn(false);
        assertFalse((Boolean)service.access(actor).get("canManageSite"));
        assertThrows(AccessDeniedException.class,()->service.update("frontend",null,Map.of(),"",actor,null));
        when(guard.hasAny(actor,"site:view","seo:view")).thenReturn(false);
        assertThrows(AccessDeniedException.class,()->service.read("site",actor));
    }
}
