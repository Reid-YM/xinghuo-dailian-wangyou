package com.xinghuo.guide;

import com.xinghuo.exception.NotFoundException;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XingweiGuideWorkspaceService {
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final GuideContentService guides;
    public XingweiGuideWorkspaceService(JdbcTemplate jdbc,XingweiPermissionGuard guard,GuideContentService guides){this.jdbc=jdbc;this.guard=guard;this.guides=guides;}
    public Map<String,Object> access(Authentication actor){readPermission(actor);return Map.of("canManage",canManage(actor));}
    @Transactional(readOnly=true)
    public List<GuideModels.GuideSummary> list(Authentication actor){readPermission(actor);return guides.adminList();}
    @Transactional(readOnly=true)
    public Document read(long id,Authentication actor){readPermission(actor);return document(id,guides.adminDetail(id));}
    @Transactional
    public Document save(Long id,GuideModels.GuideUpsert body,String expected,Authentication actor,HttpServletRequest request){
        writePermission(actor);if(id!=null)checkVersion(id,expected);
        GuideModels.GuideDetail result=id==null?guides.create(body,actor.getName(),request):guides.update(id,body,actor.getName(),request);
        return document(result.guide().id(),result);
    }
    @Transactional
    public Document status(long id,GuideModels.GuideStatusInput body,String expected,Authentication actor,HttpServletRequest request){
        writePermission(actor);checkVersion(id,expected);return document(id,guides.setStatus(id,body.status(),body.reason(),actor.getName(),request));
    }
    @Transactional
    public void archive(long id,String expected,Authentication actor,HttpServletRequest request){writePermission(actor);checkVersion(id,expected);guides.archive(id,"新版后台归档攻略",actor.getName(),request);}
    @Transactional
    public GuideModels.GuideBatchArchiveResult batch(GuideModels.GuideBatchArchiveRequest body,Authentication actor,HttpServletRequest request){
        writePermission(actor);return guides.archiveBatch(body==null?List.of():body.ids(),body==null?"新版后台批量归档":body.reason(),actor.getName(),request);
    }
    @Transactional
    public GuideModels.GuideBatchArchiveResult archiveAll(GuideModels.GuideBatchArchiveRequest body,Authentication actor,HttpServletRequest request){
        writePermission(actor);return guides.archiveAll(body==null?"新版后台全部归档":body.reason(),actor.getName(),request);
    }
    @Transactional
    public GuideModels.GuideImportResult importGames(GuideModels.GuideImportRequest body,Authentication actor,HttpServletRequest request){
        writePermission(actor);return guides.importGames(body==null?new GuideModels.GuideImportRequest(true,false,"新版后台生成攻略"):body,actor.getName(),request);
    }
    public boolean canManage(Authentication actor){var a=guard.resolve(actor);return !"DENIED".equals(a.mode())&&guard.has(actor,"content:manage")&&(a.allowAll()||"ALL".equals(a.dataScope()));}
    private void readPermission(Authentication actor){if("DENIED".equals(guard.resolve(actor).mode())||!guard.has(actor,"content:view"))throw new AccessDeniedException("没有攻略查看权限");}
    private void writePermission(Authentication actor){if(!canManage(actor))throw new AccessDeniedException("全站攻略写入需要内容管理权限与全部数据范围");}
    private Document document(long id,GuideModels.GuideDetail detail){Long version=jdbc.queryForObject("SELECT version FROM game_guides WHERE id=?",Long.class,id);return new Document(detail,token(id,version));}
    private void checkVersion(long id,String expected){
        List<Long> versions=jdbc.queryForList("SELECT version FROM game_guides WHERE id=? FOR UPDATE",Long.class,id);
        if(versions.isEmpty())throw new NotFoundException();String token=token(id,versions.getFirst());
        if(expected==null||(!token.equals(expected)&&!("\""+token+"\"").equals(expected)))throw new OptimisticLockingFailureException("攻略已被更新，请重新读取后核对保存；当前输入未保存");
    }
    private static String token(long id,long version){return "guide-"+id+"-v"+version;}
    public record Document(GuideModels.GuideDetail detail,String revision){}
}
