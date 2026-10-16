package com.xinghuo.guide;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/xingwei/guides")
@PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:view')")
public class XingweiGuideController {
    private final XingweiGuideWorkspaceService work;
    private final TaxonomyService taxonomy;
    private final ServiceCatalogService services;
    public XingweiGuideController(XingweiGuideWorkspaceService work,TaxonomyService taxonomy,ServiceCatalogService services){this.work=work;this.taxonomy=taxonomy;this.services=services;}
    @GetMapping List<GuideModels.GuideSummary> list(Authentication actor){return work.list(actor);}
    @GetMapping("/access") Map<String,Object> access(Authentication actor){return work.access(actor);}
    @GetMapping("/catalog") ApiDtos.CatalogResponse catalog(){return taxonomy.catalog(true);}
    @GetMapping("/services") List<Map<String,Object>> services(){return services.adminList().stream().map(s->Map.<String,Object>of("id",s.id(),"gameCode",s.gameCode(),"name",s.name(),"shortDesc",s.shortDesc()==null?"":s.shortDesc(),"active",s.active(),"status",s.status())).toList();}
    @GetMapping("/{id}") ResponseEntity<GuideModels.GuideDetail> detail(@PathVariable long id,Authentication actor){return response(work.read(id,actor));}
    @PostMapping @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    ResponseEntity<GuideModels.GuideDetail> create(@RequestBody GuideModels.GuideUpsert body,Authentication actor,HttpServletRequest request){return response(work.save(null,body,null,actor,request));}
    @PutMapping("/{id}") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    ResponseEntity<GuideModels.GuideDetail> update(@PathVariable long id,@RequestBody GuideModels.GuideUpsert body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return response(work.save(id,body,revision,actor,request));}
    @PatchMapping("/{id}/status") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    ResponseEntity<GuideModels.GuideDetail> status(@PathVariable long id,@RequestBody GuideModels.GuideStatusInput body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return response(work.status(id,body,revision,actor,request));}
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    void archive(@PathVariable long id,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){work.archive(id,revision,actor,request);}
    @DeleteMapping("/batch") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    GuideModels.GuideBatchArchiveResult batch(@RequestBody GuideModels.GuideBatchArchiveRequest body,Authentication actor,HttpServletRequest request){return work.batch(body,actor,request);}
    @PostMapping("/archive-all") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    GuideModels.GuideBatchArchiveResult archiveAll(@RequestBody(required=false) GuideModels.GuideBatchArchiveRequest body,Authentication actor,HttpServletRequest request){return work.archiveAll(body,actor,request);}
    @PostMapping("/import-games") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'content:manage')")
    GuideModels.GuideImportResult importGames(@RequestBody(required=false) GuideModels.GuideImportRequest body,Authentication actor,HttpServletRequest request){return work.importGames(body,actor,request);}
    private ResponseEntity<GuideModels.GuideDetail> response(XingweiGuideWorkspaceService.Document document){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(document.revision()).body(document.detail());}
    @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN) Map<String,Object> forbidden(AccessDeniedException e){return Map.of("error",e.getMessage());}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseStatus(HttpStatus.CONFLICT) Map<String,Object> conflict(OptimisticLockingFailureException e){return Map.of("error",e.getMessage());}
}
