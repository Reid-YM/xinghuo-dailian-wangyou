package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.XingweiCatalogRevisionService.Kind;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.multipart.MultipartFile;

/** Full catalogue metadata complements controlled visibility/publish workflows. */
@Service
public class XingweiCatalogDetailsService {
    private final XingweiPermissionGuard guard;private final ServiceCatalogService services;private final TaxonomyService taxonomy;private final XingweiCatalogRevisionService revisions;private final EntityManager entities;private final UploadService uploads;private final XingweiServicePublishReviewService publish;private final XingweiTaxonomyControlService visibility;private final AuditService audit;
    public XingweiCatalogDetailsService(XingweiPermissionGuard guard,ServiceCatalogService services,TaxonomyService taxonomy,XingweiCatalogRevisionService revisions,EntityManager entities,UploadService uploads,XingweiServicePublishReviewService publish,XingweiTaxonomyControlService visibility,AuditService audit){this.guard=guard;this.services=services;this.taxonomy=taxonomy;this.revisions=revisions;this.entities=entities;this.uploads=uploads;this.publish=publish;this.visibility=visibility;this.audit=audit;}
    private void require(Authentication actor,String permission){if(!guard.has(actor,permission))throw new AccessDeniedException("没有目录操作权限");}
    private void global(Authentication actor,String permission){require(actor,permission);var access=guard.resolve(actor);if("DENIED".equals(access.mode())||(!access.allowAll()&&!"ALL".equals(access.dataScope())))throw new AccessDeniedException("完整目录维护需要全部数据范围");}
    @Transactional(readOnly=true) public Map<String,Object> snapshot(Authentication actor){
        require(actor,"service:view");var catalog=taxonomy.catalog(true);var access=guard.resolve(actor);
        return Map.of("services",services.adminList().stream().map(item->new State(item,revisions.current(Kind.SERVICE,item.id()))).toList(),
            "games",catalog.games().stream().map(item->new State(item,revisions.current(Kind.GAME,item.code()))).toList(),
            "categories",catalog.categories().stream().map(item->new State(item,revisions.current(Kind.CATEGORY,item.id()))).toList(),
            "platforms",catalog.platforms(),"canManage",guard.has(actor,"service:manage")&&(access.allowAll()||"ALL".equals(access.dataScope())),"restoreAdmin",access.allowAll());
    }
    @Transactional(readOnly=true) public State game(String code){var row=taxonomy.catalog(true).games().stream().filter(item->item.code().equals(taxonomy.normalizeGame(code))).findFirst().orElseThrow(NotFoundException::new);return new State(row,revisions.current(Kind.GAME,row.code()));}
    @Transactional(readOnly=true) public State category(long id){var row=taxonomy.catalog(true).categories().stream().filter(item->item.id()==id).findFirst().orElseThrow(NotFoundException::new);return new State(row,revisions.current(Kind.CATEGORY,id));}
    @Transactional(isolation=Isolation.READ_COMMITTED) public State updateService(long id,ApiDtos.ServiceUpsert body,String expected,Authentication actor,HttpServletRequest request){
        global(actor,"service:manage");reason(body.reason());revisions.lock(Kind.SERVICE,id,expected);var before=services.adminList().stream().filter(item->item.id()==id).findFirst().orElseThrow(NotFoundException::new);
        String status=body.status().toUpperCase(Locale.ROOT);if(!Set.of("AVAILABLE","BUSY","OFFLINE").contains(status))throw new BadRequestException("服务状态无效");
        if(before.active()&&!"OFFLINE".equalsIgnoreCase(before.status())){if("OFFLINE".equals(status))throw new BadRequestException("停用请使用目录的下线操作");}
        if(before.active()&&"OFFLINE".equalsIgnoreCase(before.status())&&!"OFFLINE".equals(status))throw new BadRequestException("离线服务请从目录的发布检查启用");
        var safe=new ApiDtos.ServiceUpsert(body.gameCode(),body.slug(),body.name(),body.category(),body.shortDesc(),body.priceMode(),body.priceFixed(),body.priceMin(),body.priceMax(),body.durationText(),status,body.coverImage(),body.hotScore(),body.manualPriority(),body.expiresAt(),before.active(),body.sortOrder(),body.reason());
        services.update(id,safe,actor.getName(),request);entities.flush();if(before.active()&&!"OFFLINE".equalsIgnoreCase(before.status())&&(body.expiresAt()==null||body.expiresAt().isAfter(java.time.Instant.now())))publish.requireReady(id);
        var after=services.adminList().stream().filter(item->item.id()==id).findFirst().orElseThrow(NotFoundException::new);return new State(after,revisions.current(Kind.SERVICE,id));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public State updateGame(String code,ApiDtos.GameProfileUpdateRequest body,String expected,Authentication actor,HttpServletRequest request){global(actor,"service:manage");reason(body.reason());code=taxonomy.normalizeGame(code);revisions.lock(Kind.GAME,code,expected);var before=(ApiDtos.GameOptionResponse)game(code).data();var safe=new ApiDtos.GameProfileUpdateRequest(body.name(),body.nameEn(),body.aliases(),body.iconKey(),body.description(),body.publisher(),body.genres(),body.logoImage(),body.coverImage(),body.popularityScore(),body.featured(),body.businessStatus(),body.seoSlug(),body.seoTitle(),body.seoDescription(),body.sortOrder(),before.active(),body.platformCodes(),body.reason());taxonomy.updateGameProfile(code,safe,actor.getName(),request);entities.flush();return game(code);}
    @Transactional(isolation=Isolation.READ_COMMITTED) public void delete(String kind,String key,Confirmation body,String expected,Authentication actor,HttpServletRequest request){global(actor,"service:manage");if(!guard.resolve(actor).allowAll())throw new AccessDeniedException("永久删除仅限恢复 ADMIN");if(!Objects.equals(key,body.confirmation()))throw new BadRequestException("请准确输入要删除的编号");if("games".equals(kind)){String code=taxonomy.normalizeGame(key);revisions.lock(Kind.GAME,code,expected);taxonomy.deleteGame(code,actor.getName(),request);}else if("categories".equals(kind)){long id;try{id=Long.parseLong(key);}catch(NumberFormatException invalid){throw new BadRequestException("分类编号无效");}revisions.lock(Kind.CATEGORY,id,expected);taxonomy.deleteCategory(id,actor.getName(),request);}else throw new BadRequestException("不支持的删除类型");entities.flush();}
    public ApiDtos.SiteImageUploadResponse image(String kind,MultipartFile file,Authentication actor,HttpServletRequest request){global(actor,"service:manage");var type=switch(kind){case "service"->UploadAsset.Kind.SERVICE_COVER;case "logo"->UploadAsset.Kind.GAME_LOGO;case "cover"->UploadAsset.Kind.GAME_COVER;default->throw new BadRequestException("图片类型无效");};audit.log("XW_CATALOG_IMAGE_REQUEST","CATALOG",kind,actor.getName(),null,Map.of("requested",true),"上传目录图片，待保存引用",request);var asset=uploads.storeImage(file,type);return new ApiDtos.SiteImageUploadResponse("/api/site-assets/"+asset.getId());}
    private void reason(String value){if(value==null||value.isBlank()||value.length()>500)throw new BadRequestException("请填写不超过500字的修改说明");}
    @Transactional(isolation=Isolation.READ_COMMITTED) public void bulk(BulkInput body,Authentication actor,HttpServletRequest request){global(actor,"service:manage");var items=body.items().stream().map(item->new BulkItem(taxonomy.normalizeGame(item.code()),item.revision(),item.impactToken())).sorted(Comparator.comparing(BulkItem::code)).toList();if(items.stream().map(item->taxonomy.normalizeGame(item.code())).distinct().count()!=items.size())throw new BadRequestException("游戏编号不能重复");if(body.active()==null&&body.featured()==null&&body.businessStatus()==null&&body.popularityScore()==null)throw new BadRequestException("请至少选择一项需要修改的资料");for(var item:items)revisions.lock(Kind.GAME,taxonomy.normalizeGame(item.code()),item.revision());
        if(body.active()!=null){if(body.featured()!=null||body.businessStatus()!=null||body.popularityScore()!=null)throw new BadRequestException("批量启停须单独预览，不能与资料修改同时执行");for(var item:items){var row=(ApiDtos.GameOptionResponse)game(item.code()).data();if(row.active()!=body.active())visibility.setGameActive(item.code(),body.active(),body.reason(),item.impactToken(),actor.getName(),request);}}
        else taxonomy.bulkUpdateGames(new ApiDtos.GameBulkUpdateRequest(items.stream().map(BulkItem::code).toList(),null,body.featured(),body.businessStatus(),body.popularityScore(),body.reason()),actor.getName(),request);entities.flush();}
    public record State(Object data,String revision){} public record Confirmation(@NotBlank String confirmation){}
    public record BulkItem(@NotBlank @Size(max=32) String code,@NotBlank String revision,String impactToken){}
    public record BulkInput(@NotEmpty @Size(max=100) List<@jakarta.validation.Valid BulkItem> items,Boolean active,Boolean featured,@Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") String businessStatus,@Min(0) @Max(100) Integer popularityScore,@NotBlank @Size(max=500) String reason){}
}
