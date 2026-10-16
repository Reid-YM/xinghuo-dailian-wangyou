package com.xinghuo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.*;
import jakarta.servlet.http.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Native entry points for the remaining global content and traffic tools. */
@Controller
public class XingweiAuxiliaryWorkspaceController {
    private final XingweiPermissionGuard guard;private final ToolStationService tools;private final UploadService uploads;private final AuditService audits;private final AdminTrackingController tracking;private final JdbcTemplate jdbc;
    public XingweiAuxiliaryWorkspaceController(XingweiPermissionGuard guard,ToolStationService tools,UploadService uploads,AuditService audits,AdminTrackingController tracking,JdbcTemplate jdbc){this.guard=guard;this.tools=tools;this.uploads=uploads;this.audits=audits;this.tracking=tracking;this.jdbc=jdbc;}
    @ModelAttribute void privateResponse(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private boolean allowed(Authentication actor,String permission){var access=guard.resolve(actor);return !"DENIED".equals(access.mode())&&guard.has(actor,permission)&&(access.allowAll()||"ALL".equals(access.dataScope()));}
    private void require(Authentication actor,String permission){if(!allowed(actor,permission))throw new AccessDeniedException("此全站模块需要对应权限及全部数据范围");}
    @GetMapping("/admin/xingwei/tool-station") String tools(Model model,CsrfToken csrf,Authentication actor){require(actor,"content:view");model.addAttribute("csrf",csrf);model.addAttribute("xingweiToolMode",true);return "admin-xingwei-tool-station";}
    @GetMapping("/admin/xingwei/statistics") String statistics(Authentication actor){require(actor,"dashboard:view");return "admin-xingwei-statistics";}
    @GetMapping("/api/admin/xingwei/tool-station/access") @ResponseBody Object access(Authentication actor){require(actor,"content:view");return Map.of("canManage",allowed(actor,"content:manage"));}
    @GetMapping("/api/admin/xingwei/tool-station") @ResponseBody ResponseEntity<JsonNode> read(Authentication actor){require(actor,"content:view");return data(tools.publicData());}
    @PutMapping("/api/admin/xingwei/tool-station") @ResponseBody ResponseEntity<JsonNode> save(@RequestBody JsonNode body,@RequestHeader(value="If-Match",required=false) String expected,@RequestParam String reason,Authentication actor,HttpServletRequest request){require(actor,"content:manage");return data(tools.checkedWrite(body,expected,false,()->auditWrite(actor,reason,false,request)));}
    @PostMapping("/api/admin/xingwei/tool-station/reset") @ResponseBody ResponseEntity<JsonNode> reset(@RequestHeader(value="If-Match",required=false) String expected,@RequestParam String reason,Authentication actor,HttpServletRequest request){require(actor,"content:manage");return data(tools.checkedWrite(null,expected,true,()->auditWrite(actor,reason,true,request)));}
    @PostMapping("/api/admin/xingwei/tool-station/image") @ResponseBody Object image(@RequestParam MultipartFile file,Authentication actor,HttpServletRequest request){require(actor,"content:manage");audits.log("XW_TOOL_IMAGE_REQUEST","TOOL_STATION","public",actor.getName(),null,Map.of("requested",true),"上传工具站图片，待保存引用",request);var asset=uploads.storeImage(file,UploadAsset.Kind.TOOL_STATION_COVER);return new ApiDtos.SiteImageUploadResponse("/api/site-assets/"+asset.getId());}
    @GetMapping("/api/admin/xingwei/statistics") @ResponseBody Object stats(@RequestParam(defaultValue="7") int days,Authentication actor){
        require(actor,"dashboard:view");var result=new LinkedHashMap<String,Object>(tracking.summary(days));var operations=new ArrayList<Map<String,Object>>();
        for(var metric:List.of(new String[]{"service:view","服务项目","SELECT COUNT(*) FROM services"},new String[]{"quote:view","新询价","SELECT COUNT(*) FROM quotes WHERE status='NEW'"},new String[]{"order:view","履约中","SELECT COUNT(*) FROM customer_orders WHERE status='IN_PROGRESS'"},new String[]{"order:view","已完成","SELECT COUNT(*) FROM customer_orders WHERE status='COMPLETED'"},new String[]{"feedback:view","新反馈","SELECT COUNT(*) FROM admin_test_feedback WHERE status='NEW'"})){
            if(guard.has(actor,metric[0])){Long count=jdbc.queryForObject(metric[2],Long.class);operations.add(Map.of("label",metric[1],"count",count==null?0:count));}
        }
        result.put("operations",operations);return result;
    }
    private ResponseEntity<JsonNode> data(JsonNode data){return ResponseEntity.ok().eTag(tools.revision(data)).body(data);}
    private void auditWrite(Authentication actor,String reason,boolean reset,HttpServletRequest request){if(reason==null||reason.isBlank()||reason.length()>500)throw new BadRequestException("请填写不超过500字的修改说明");audits.log("XW_TOOL_WRITE_REQUEST","TOOL_STATION","public",actor.getName(),null,Map.of("reset",reset,"requested",true),reason.trim(),request);}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseBody ResponseEntity<?> conflict(OptimisticLockingFailureException error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) @ResponseBody ResponseEntity<?> denied(AccessDeniedException error){return ResponseEntity.status(403).body(Map.of("message",error.getMessage()));}
}
