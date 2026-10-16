package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiCatalogDetailsServiceTest {
    JdbcTemplate jdbc;XingweiPermissionGuard guard;ServiceCatalogService services;TaxonomyService taxonomy;UploadService uploads;XingweiServicePublishReviewService publish;XingweiTaxonomyControlService visibility;XingweiCatalogRevisionService revisions;XingweiCatalogDetailsService work;ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    Authentication admin,manager,narrow,viewer;AtomicReference<ApiDtos.ServiceUpsert> saved=new AtomicReference<>();
    @BeforeEach void setup()throws Exception{
        var ds=new DriverManagerDataSource("jdbc:h2:mem:catalogdetails"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE services(id BIGINT PRIMARY KEY,updated_at TIMESTAMP(6),version BIGINT)");jdbc.update("INSERT INTO services VALUES(1,CURRENT_TIMESTAMP,3)");jdbc.execute("CREATE TABLE game_catalog(code VARCHAR(32) PRIMARY KEY,updated_at TIMESTAMP(6))");jdbc.update("INSERT INTO game_catalog VALUES('MW4',CURRENT_TIMESTAMP)");jdbc.execute("CREATE TABLE service_categories(id BIGINT PRIMARY KEY,updated_at TIMESTAMP(6))");jdbc.update("INSERT INTO service_categories VALUES(9,CURRENT_TIMESTAMP)");
        guard=mock(XingweiPermissionGuard.class);services=mock(ServiceCatalogService.class);taxonomy=mock(TaxonomyService.class);uploads=mock(UploadService.class);publish=mock(XingweiServicePublishReviewService.class);visibility=mock(XingweiTaxonomyControlService.class);var entities=mock(EntityManager.class);revisions=new XingweiCatalogRevisionService(jdbc,entities);
        admin=actor("admin","ALL",true,true);manager=actor("manager","ALL",true,false);narrow=actor("narrow","DEPARTMENT",true,false);viewer=actor("viewer","ALL",false,false);
        var game=json.readValue("{\"code\":\"MW4\",\"name\":\"游戏\",\"active\":false,\"businessStatus\":\"READY\",\"platformCodes\":[\"PC\"]}",ApiDtos.GameOptionResponse.class);
        when(taxonomy.catalog(true)).thenReturn(new ApiDtos.CatalogResponse(List.of(game),List.of(),List.of(),List.of()));when(taxonomy.normalizeGame(anyString())).thenAnswer(call->call.getArgument(0,String.class).toUpperCase());
        when(services.adminList()).thenAnswer(call->List.of(json.readValue("{\"id\":1,\"slug\":\"actual-service\",\"gameCode\":\"MW4\",\"name\":\"服务\",\"category\":\"TASK\",\"shortDesc\":\"说明\",\"priceMode\":\"FIXED\",\"priceFixed\":88,\"durationText\":\"1天\",\"status\":\"AVAILABLE\",\"active\":true,\"version\":"+jdbc.queryForObject("SELECT version FROM services WHERE id=1",Long.class)+"}",ApiDtos.AdminServiceResponse.class)));
        when(services.update(eq(1L),any(),anyString(),isNull())).thenAnswer(call->{saved.set(call.getArgument(1));jdbc.update("UPDATE services SET version=version+1,updated_at=DATEADD('SECOND',1,updated_at) WHERE id=1");return services.adminList().getFirst();});
        var target=new XingweiCatalogDetailsService(guard,services,taxonomy,revisions,entities,uploads,publish,visibility,mock(AuditService.class));var tx=new TransactionInterceptor();tx.setTransactionManager(new DataSourceTransactionManager(ds));tx.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());var factory=new ProxyFactory(target);factory.addAdvice(tx);work=(XingweiCatalogDetailsService)factory.getProxy();
    }
    Authentication actor(String name,String scope,boolean manage,boolean override){Authentication actor=new UsernamePasswordAuthenticationToken(name,"",List.of());when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",override,List.of(),List.of(),scope,1L,"03"));when(guard.has(actor,"service:view")).thenReturn(true);when(guard.has(actor,"service:manage")).thenReturn(manage);return actor;}
    ApiDtos.ServiceUpsert input()throws Exception{return json.readValue("{\"gameCode\":\"MW4\",\"slug\":\"actual-service\",\"name\":\"服务更新\",\"category\":\"TASK\",\"shortDesc\":\"说明\",\"priceMode\":\"FIXED\",\"priceFixed\":88,\"durationText\":\"1天\",\"status\":\"BUSY\",\"active\":false,\"coverImage\":\"/api/site-assets/test-image\",\"hotScore\":9.5,\"expiresAt\":\"2030-01-01T00:00:00Z\",\"reason\":\"维护完整资料\"}",ApiDtos.ServiceUpsert.class);}
    @Test void metadataPreservesActivationButMigratesCoverExpiryHeatAndBusyState()throws Exception{
        var result=work.updateService(1,input(),"\"v3\"",manager,null);assertEquals("v4",result.revision());assertEquals(true,saved.get().active());assertEquals("BUSY",saved.get().status());assertEquals("/api/site-assets/test-image",saved.get().coverImage());assertEquals("2030-01-01T00:00:00Z",saved.get().expiresAt().toString());assertEquals(0,saved.get().hotScore().compareTo(new java.math.BigDecimal("9.5")));verify(publish).requireReady(1L);
    }
    @Test void staleOrUnreadyUpdatesDoNotCommitServiceChanges()throws Exception{
        assertThrows(OptimisticLockingFailureException.class,()->work.updateService(1,input(),"v2",manager,null));verify(services,never()).update(anyLong(),any(),any(),any());
        doThrow(new BadRequestException("发布条件不足")).when(publish).requireReady(1L);assertThrows(BadRequestException.class,()->work.updateService(1,input(),"v3",manager,null));assertEquals(3L,jdbc.queryForObject("SELECT version FROM services WHERE id=1",Long.class));
    }
    @Test void fullGameProfileCannotChangeActivationAndOnlyAdminCanDelete()throws Exception{
        var game=work.game("MW4");var input=json.readValue("{\"name\":\"游戏完整资料\",\"nameEn\":\"Actual Game\",\"aliases\":\"别名\",\"logoImage\":\"/logo.png\",\"coverImage\":\"/cover.png\",\"featured\":true,\"active\":true,\"businessStatus\":\"READY\",\"platformCodes\":[\"PC\",\"PS5\"],\"reason\":\"资料维护\"}",ApiDtos.GameProfileUpdateRequest.class);
        work.updateGame("MW4",input,game.revision(),manager,null);var captured=org.mockito.ArgumentCaptor.forClass(ApiDtos.GameProfileUpdateRequest.class);verify(taxonomy).updateGameProfile(eq("MW4"),captured.capture(),eq("manager"),isNull());assertEquals(false,captured.getValue().active());assertEquals(List.of("PC","PS5"),captured.getValue().platformCodes());assertEquals("/cover.png",captured.getValue().coverImage());
        assertThrows(AccessDeniedException.class,()->work.delete("games","MW4",new XingweiCatalogDetailsService.Confirmation("MW4"),game.revision(),manager,null));assertThrows(BadRequestException.class,()->work.delete("games","MW4",new XingweiCatalogDetailsService.Confirmation("WRONG"),game.revision(),admin,null));verify(taxonomy,never()).deleteGame(any(),any(),any());
    }
    @Test void readOnlyAndNarrowRolesCannotMutateFullProfilesOrUpload()throws Exception{
        assertEquals(false,work.snapshot(narrow).get("canManage"));assertThrows(AccessDeniedException.class,()->work.updateService(1,input(),"v3",narrow,null));assertThrows(AccessDeniedException.class,()->work.updateService(1,input(),"v3",viewer,null));assertThrows(AccessDeniedException.class,()->work.image("service",null,narrow,null));verifyNoInteractions(uploads,publish);
    }
    @Test void basicAndFullEditorsShareTheSameServiceRevision(){
        assertEquals("v3",revisions.current(XingweiCatalogRevisionService.Kind.SERVICE,1));jdbc.update("UPDATE services SET version=4 WHERE id=1");assertThrows(OptimisticLockingFailureException.class,()->revisions.lock(XingweiCatalogRevisionService.Kind.SERVICE,1,"v3"));
    }
    @Test void basicEditorCannotOverwriteFullMetadataOrBypassPublishing()throws Exception{
        var controller=new com.xinghuo.controller.XingweiServiceCatalogController(services,taxonomy,publish,visibility,revisions,work);
        controller.updateService(1L,input(),"v3",manager,null);assertEquals("AVAILABLE",saved.get().status());assertEquals(true,saved.get().active());assertNull(saved.get().coverImage());assertNull(saved.get().expiresAt());assertEquals("actual-service",saved.get().slug());
    }
    @Test void creatingThroughNativeBasicApiCannotPublishAClientSuppliedActiveService()throws Exception{
        var controller=new com.xinghuo.controller.XingweiServiceCatalogController(services,taxonomy,publish,visibility,revisions,work);
        controller.createService(input(),manager,null);var captured=org.mockito.ArgumentCaptor.forClass(ApiDtos.ServiceUpsert.class);verify(services).create(captured.capture(),eq("manager"),isNull());assertEquals(false,captured.getValue().active());assertEquals("OFFLINE",captured.getValue().status());
    }
    @Test void bulkChecksSharedRevisionAndRequiresSeparateVisibilityPreview(){
        String revision=work.game("MW4").revision();var item=new XingweiCatalogDetailsService.BulkItem("MW4",revision,"review-token");
        work.bulk(new XingweiCatalogDetailsService.BulkInput(List.of(item),true,null,null,null,"核对影响后启用"),manager,null);verify(visibility).setGameActive("MW4",true,"核对影响后启用","review-token","manager",null);
        assertThrows(BadRequestException.class,()->work.bulk(new XingweiCatalogDetailsService.BulkInput(List.of(item),false,true,null,null,"不可混用"),manager,null));
        assertThrows(BadRequestException.class,()->work.bulk(new XingweiCatalogDetailsService.BulkInput(List.of(item,new XingweiCatalogDetailsService.BulkItem("mw4",revision,null)),null,true,null,null,"重复编码"),manager,null));
        jdbc.update("UPDATE game_catalog SET updated_at=DATEADD('SECOND',1,updated_at) WHERE code='MW4'");assertThrows(OptimisticLockingFailureException.class,()->work.bulk(new XingweiCatalogDetailsService.BulkInput(List.of(item),null,true,null,null,"过期资料"),manager,null));verify(taxonomy,never()).bulkUpdateGames(any(),any(),any());
    }
}
