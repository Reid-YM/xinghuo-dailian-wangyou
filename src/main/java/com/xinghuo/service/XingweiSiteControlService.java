package com.xinghuo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.model.UploadAsset;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** New permission-controlled entry to the existing configuration sources; legacy routes remain unchanged. */
@Service
public class XingweiSiteControlService {
    private static final Map<String,List<String>> SECTIONS=Map.ofEntries(
        Map.entry("basic",List.of("brandName","legalEntityName","serviceStatus","statusShortText","primaryButtonLabel","quoteEnabled","orderQueryEnabled")),
        Map.entry("home",List.of("heroTitle","heroSubtitle","startingPriceMode","startingPrice","showStartingPrice","homeServiceLimit","trustTags")),
        Map.entry("visual",List.of("heroImage","heroImageHeight","heroImageFit","heroImagePositionX","heroImagePositionY","heroBackgroundImage","heroBackgroundOpacity","heroBackgroundFit","heroBackgroundPositionX","heroBackgroundPositionY","heroBackgroundBlur","serviceCoverHeight","serviceCoverFit","serviceCoverPositionX","serviceCoverPositionY")),
        Map.entry("notice",List.of("announcementEnabled","announcementText")),
        Map.entry("contact",List.of("contactTitle","contactIntro","wechatId","wechatQrImage","wechatQrSize","contactChannels","qqId","qqLink","phone","serviceHours","taobaoUrl","xianyuUrl","contactNotice","wecomKefuUrl","wecomKefuNote","quickContactTitle","taobaoLabel","xianyuLabel","wecomKefuLabel","wecomKefuButton")),
        Map.entry("trust",List.of("trustCases","trustFaq")),
        Map.entry("copy",List.of("quoteSuccessText","orderHelpText","footerText")),
        Map.entry("seo",List.of("seoTitle","seoDescription")),
        Map.entry("tools",List.of("toolUpdatedNote","toolPublicEnabled")));
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final SiteConfigService config;
    private final SiteSettingService settings;
    private final FrontendControlService frontend;
    private final GameStatusService games;
    private final AuditService audit;
    private final ObjectMapper mapper;
    private final UploadService uploads;
    public XingweiSiteControlService(JdbcTemplate jdbc,XingweiPermissionGuard guard,SiteConfigService config,
            SiteSettingService settings,FrontendControlService frontend,GameStatusService games,AuditService audit,ObjectMapper mapper,UploadService uploads){
        this.jdbc=jdbc;this.guard=guard;this.config=config;this.settings=settings;this.frontend=frontend;this.games=games;this.audit=audit;this.mapper=mapper;this.uploads=uploads;
    }
    public Map<String,Object> access(Authentication actor){
        requireRead(actor,"site");
        return Map.of("canManageSite",canWrite(actor,"site:manage"),"canManageSeo",canWrite(actor,"seo:manage"),"canViewSite",guard.has(actor,"site:view"));
    }
    @Transactional(readOnly=true)
    public State read(String area,Authentication actor){requireRead(actor,area);return state(values(area));}
    @Transactional
    public State update(String area,String section,Map<String,Object> body,String expected,Authentication actor,HttpServletRequest request){
        String permission="site".equals(area)&&"seo".equals(section)?"seo:manage":"site:manage";
        if(!canWrite(actor,permission))throw new AccessDeniedException("只有全部数据范围的对应配置管理员可保存全站配置");
        if(body==null)throw new IllegalArgumentException("配置内容不能为空");
        if(body.get("reason")!=null&&String.valueOf(body.get("reason")).length()>500)throw new IllegalArgumentException("修改原因不能超过500字");
        // A shared existing singleton serializes new-console writers without a schema change.
        jdbc.queryForObject("SELECT id FROM site_config WHERE id=1 FOR UPDATE",Long.class);
        Object before=values(area);String revision=state(before).revision();
        if(expected==null||(!expected.equals(revision)&&!expected.equals('"'+revision+'"')))
            throw new OptimisticLockingFailureException("配置已变化，请重新读取后核对并保存；当前输入已保留");
        if("site".equals(area)){
            List<String> keys=SECTIONS.get(section);if(keys==null)throw new IllegalArgumentException("配置分区无效");
            allowed(body,Set.copyOf(keys));
            validateUrls(body);
            validateCollections(body);
            Map<String,Object> current=mapper.convertValue(before,new TypeReference<>(){});
            Map<String,Object> merged=new LinkedHashMap<>();for(String key:keys)merged.put(key,current.get(key));merged.putAll(body);
            config.updateSection(section,merged,actor.getName(),request);
        }else if("global-ui".equals(area)||"frontend".equals(area)){
            @SuppressWarnings("unchecked") Map<String,String> current=(Map<String,String>)before;
            allowed(body,current.keySet());
            Map<String,Object> copy=new LinkedHashMap<>(body);Object reason=copy.remove("reason");
            Object after="global-ui".equals(area)?settings.updateGlobalUiSettings(copy):frontend.update(copy);
            audit.log("XINGWEI_UI_UPDATE","SITE_SETTINGS",area,actor.getName(),before,after,
                    reason==null?"新版前台控制保存":String.valueOf(reason).substring(0,Math.min(500,String.valueOf(reason).length())),request);
        }else if("games".equals(area)){
            allowed(body,Set.of("statusCode","publicLabel","publicDetail","ctaLabel"));
            games.update(section,str(body.get("statusCode")),str(body.get("publicLabel")),str(body.get("publicDetail")),str(body.get("ctaLabel")),actor.getName(),request,str(body.get("reason")));
        }else throw new IllegalArgumentException("配置类型无效");
        return state(values(area));
    }
    @Transactional
    public Map<String,String> upload(String field,MultipartFile file,Authentication actor,HttpServletRequest request){
        if(!canWrite(actor,"site:manage"))throw new AccessDeniedException("没有全站图片管理权限");
        UploadAsset.Kind kind=switch(field){case "heroImage"->UploadAsset.Kind.SITE_HERO;case "heroBackgroundImage"->UploadAsset.Kind.SITE_BACKGROUND;case "wechatQrImage","qrImage"->UploadAsset.Kind.SITE_WECHAT_QR;default->throw new IllegalArgumentException("图片位置无效");};
        UploadAsset asset=uploads.storeImage(file,kind);
        uploads.bind(asset.getId(),"SITE_CONFIG",field);
        audit.log("XINGWEI_SITE_ASSET_UPLOAD","SITE_ASSET",String.valueOf(asset.getId()),actor.getName(),null,Map.of("field",field),"上传配置草稿图片",request);
        return Map.of("url","/api/site-assets/"+asset.getId());
    }
    private Object values(String area){return switch(area){case "site"->config.publicConfig();case "global-ui"->settings.globalUiSettings();case "frontend"->frontend.settings();case "games"->gameStates();default->throw new IllegalArgumentException("配置类型无效");};}
    private List<GameState> gameStates(){
        Map<String,String> names=new LinkedHashMap<>();
        jdbc.query("SELECT code,name FROM game_catalog", rs->{names.put(rs.getString(1),rs.getString(2));});
        return games.all().stream().map(g->new GameState(g.getGameCode(),names.getOrDefault(g.getGameCode(),"未命名游戏"),g.getStatusCode().name(),g.getPublicLabel(),g.getPublicDetail(),g.getCtaLabel(),g.getUpdatedAt()==null?0L:g.getUpdatedAt().toEpochMilli())).toList();
    }
    private void requireRead(Authentication actor,String area){
        boolean allowed="site".equals(area)?guard.hasAny(actor,"site:view","seo:view"):guard.has(actor,"site:view");
        if(!allowed||"DENIED".equals(guard.resolve(actor).mode()))throw new AccessDeniedException("没有配置查看权限");
    }
    private boolean canWrite(Authentication actor,String permission){
        var access=guard.resolve(actor);return !"DENIED".equals(access.mode())&&guard.has(actor,permission)&&(access.allowAll()||"ALL".equals(access.dataScope()));
    }
    private State state(Object values){
        try{Object canonical=values instanceof Map<?,?> map?new TreeMap<>(map):values;
            String revision=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(canonical).getBytes(StandardCharsets.UTF_8)));
            return new State(values,revision);
        }catch(Exception e){throw new IllegalStateException("无法读取配置版本",e);}
    }
    private static void allowed(Map<String,Object> body,Set<String> keys){
        if(body.keySet().stream().anyMatch(k->!keys.contains(k)&&!"reason".equals(k)))throw new IllegalArgumentException("包含当前分区不支持的配置项");
    }
    private static void validateCollections(Map<String,Object> body){
        if(body.containsKey("trustTags")){
            if(!(body.get("trustTags") instanceof List<?> tags)||tags.size()>6||tags.stream().anyMatch(t->!(t instanceof String)||((String)t).isBlank()||((String)t).trim().length()>30))
                throw new IllegalArgumentException("信任标签最多6项，每项1至30字");
        }
        for(String key:List.of("trustCases","trustFaq","contactChannels"))if(body.containsKey(key)){
            int max="trustCases".equals(key)?8:"trustFaq".equals(key)?12:6;
            if(!(body.get(key) instanceof List<?> rows)||rows.size()>max||rows.stream().anyMatch(row->!(row instanceof Map)))
                throw new IllegalArgumentException("案例、常见问题或客服渠道格式无效，或条目数量过多");
        }
    }
    private static void validateUrls(Map<String,Object> body){
        for(String key:List.of("heroImage","heroBackgroundImage","wechatQrImage","qqLink","taobaoUrl","xianyuUrl","wecomKefuUrl"))
            if(body.containsKey(key))safeUrl(str(body.get(key)));
        if(body.get("contactChannels") instanceof List<?> channels)for(Object row:channels)
            if(row instanceof Map<?,?> entry){safeUrl(str(entry.get("qrImage")));safeUrl(str(entry.get("jumpUrl")));}
    }
    private static void safeUrl(String value){
        if(value==null||value.isBlank())return;
        if(value.startsWith("/")&&!value.startsWith("//")&&!value.contains("\\")&&!value.contains("\r")&&!value.contains("\n"))return;
        try{var uri=java.net.URI.create(value);if(("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))&&uri.getHost()!=null&&uri.getUserInfo()==null)return;}
        catch(IllegalArgumentException ignored){}
        throw new IllegalArgumentException("图片和跳转地址必须为 http(s) 地址或以 / 开头的站内路径");
    }
    private static String str(Object value){return value==null?null:String.valueOf(value);}
    public record GameState(String gameCode,String gameName,String statusCode,String publicLabel,String publicDetail,String ctaLabel,long updatedAtMs){}
    public record State(Object values,String revision){}
}
