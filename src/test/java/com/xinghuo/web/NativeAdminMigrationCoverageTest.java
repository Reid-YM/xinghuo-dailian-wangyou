package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The recovery surface is the inventory, not a hand-selected subset of migrated menus. */
class NativeAdminMigrationCoverageTest {
    private static final Path JAVA=Path.of("src/main/java"),TEMPLATES=Path.of("src/main/resources/templates");
    private static final Pattern BASE=Pattern.compile("@RequestMapping\\s*\\(([^)]*)\\)");
    private static final Pattern METHOD=Pattern.compile("@(Get|Post|Put|Patch|Delete)Mapping(?:\\s*\\(([^)]*)\\))?");
    private static List<String> strings(String input){var out=new ArrayList<String>();var matcher=Pattern.compile("\"([^\"]*)\"").matcher(input);while(matcher.find())out.add(matcher.group(1));return out;}
    private static String base(String source){var matcher=BASE.matcher(source);return matcher.find()?strings(matcher.group(1)).getFirst():"";}
    private static Set<String> routes(String source){var out=new TreeSet<String>();var matcher=METHOD.matcher(source);while(matcher.find()){var paths=strings(Objects.requireNonNullElse(matcher.group(2),"")).stream().filter(s->s.isEmpty()||s.startsWith("/")).toList();if(paths.isEmpty())paths=List.of("");for(var path:paths)out.add(matcher.group(1).toUpperCase(Locale.ROOT)+" "+base(source)+path);}return out;}
    @Test void everyLegacyEndpointHasAnExistingNativeRouteAndWorkspace()throws Exception{
        var manifest=new ObjectMapper().readTree(Path.of("docs/admin-migration-coverage.json").toFile());
        var legacy=new TreeSet<String>();var nativeRoutes=new TreeSet<String>();var controllers=new TreeSet<String>();
        try(var files=Files.walk(JAVA)){for(var file:files.filter(p->p.toString().endsWith(".java")).toList()){
            String source=Files.readString(file),prefix=base(source),owner=file.getFileName().toString().replace(".java","");
            for(var route:routes(source))if(route.contains("/xingwei"))nativeRoutes.add(route);
            for(var route:routes(source))if(route.matches("[A-Z]+ /api/admin(?:/.*)?")&&!route.contains("/xingwei")){controllers.add(owner);legacy.add(owner+":"+route);}
        }}
        var covered=new TreeSet<String>();for(var item:manifest.get("routes")){
            assertTrue(covered.add(item.get("legacyController").asText()+":"+item.get("legacyRoute").asText()),"duplicate migration entry");
            assertFalse(item.get("nativeRoutes").isEmpty());for(var route:item.get("nativeRoutes"))assertTrue(nativeRoutes.contains(route.asText()),"missing native API: "+route);
            assertTrue(nativeRoutes.contains("GET "+item.get("workspace").asText().split("\\?")[0]),"missing native workspace: "+item);
            assertFalse(item.get("migration").asText().isBlank());
        }
        assertEquals(legacy,covered,"Every legacy endpoint, including newly added endpoints, needs a reviewed native migration");
        var declared=new TreeSet<String>();manifest.get("legacyControllers").forEach(n->declared.add(n.asText()));assertEquals(controllers,declared);
    }
    @Test void everyServerSiteConfigurationFieldHasANativeFormControl()throws Exception{
        var field=com.xinghuo.service.XingweiSiteControlService.class.getDeclaredField("SECTIONS");field.setAccessible(true);
        @SuppressWarnings("unchecked") var sections=(Map<String,List<String>>)field.get(null);
        String js=Files.readString(Path.of("src/main/resources/static/assets/admin-xingwei-site.js"));
        var controls=new HashSet<String>();var matcher=Pattern.compile("(?:text|area|check|choice|number|image|repeat)[(]'([^']+)'|key:'([^']+)'").matcher(js);
        while(matcher.find())controls.add(matcher.group(1)==null?matcher.group(2):matcher.group(1));
        for(var section:sections.entrySet())for(String key:section.getValue())assertTrue(controls.contains(key),"Missing native form control: "+section.getKey()+"/"+key);
    }
    @Test void nativeFrontendEditorIncludesEveryNavigationAndThemeSetting()throws Exception{
        var repo=org.mockito.Mockito.mock(com.xinghuo.repository.SiteSettingRepository.class);org.mockito.Mockito.when(repo.findAll()).thenReturn(List.of());
        var keys=new HashSet<>(new com.xinghuo.service.SiteSettingService(repo).globalUiSettings().keySet());
        keys.addAll(new com.xinghuo.service.FrontendControlService(repo).settings().keySet());
        String fragment=Files.readString(TEMPLATES.resolve("fragments/frontend-control-workspace.html"));
        String rollout=Files.readString(TEMPLATES.resolve("admin-xingwei-frontend-next.html"));
        for(var key:keys)assertTrue(fragment.contains("name=\""+key+"\"")||rollout.contains("name=\""+key+"\""),"Missing native frontend setting: "+key);
    }
    @Test void allNativePageShellsExcludeRetiredAdminLayoutsAndLegacyMonolithFragments()throws Exception{
        try(var files=Files.list(TEMPLATES)){for(var file:files.filter(p->p.getFileName().toString().startsWith("admin-xingwei")||p.getFileName().toString().equals("admin-login.html")).toList()){
            String html=Files.readString(file);
            for(var forbidden:List.of("/assets/app.css","admin-brand-layout-fixes","admin-light-theme","admin-control-center","/assets/admin.js","~{admin ::"))assertFalse(html.contains(forbidden),file+" loads "+forbidden);
            assertTrue(html.contains("lang=\"zh-CN\""));
        }}
        String frontend=Files.readString(Path.of("src/main/java/com/xinghuo/controller/XingweiSiteControlController.java"));
        assertTrue(frontend.contains("return \"admin-xingwei-frontend-controls\""));
        assertTrue(Files.readString(Path.of("src/main/resources/static/assets/admin-frontend-controls.js")).contains("if (!xingweiMode) ensureBrandLayoutFixes()"));
    }
}
