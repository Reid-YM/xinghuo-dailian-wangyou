package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.servlet.RequestDispatcher;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

class FrontendNextServerMetadataTest {
    @Test void everyDocumentAliasHasServerNoindexCanonicalUtf8AndNoStoreWithoutJavascript() throws Exception {
        var preview = new FrontendNextPreviewController();
        Object[] controllers = {preview, new FrontendNextN23GuidesController(preview),
                new FrontendNextN24MasteryController(preview),new FrontendNextN25SubmissionController(preview),
                new FrontendNextN26RecordsController(preview),new FrontendNextN27SupportTrustController(preview),
                new FrontendNextN28LegalController(preview)};
        var mvc = MockMvcBuilders.standaloneSetup(controllers).build();
        Set<String> paths = new LinkedHashSet<>();
        for (Object controller : controllers) for (var method : controller.getClass().getDeclaredMethods()) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            if (mapping != null) for (String path : mapping.value()) paths.add(path.replaceAll("\\{[^}]+}","test"));
        }
        assertTrue(paths.containsAll(List.of("/next", "/next/account/settings/security", "/next/records", "/next/rules-en", "/next/privacy-en")));
        for (String path : paths) {
            var result = mvc.perform(get(path).param("token","private-token").param("q","search"))
                    .andExpect(status().isOk()).andReturn().getResponse();
            String html = result.getContentAsString(StandardCharsets.UTF_8);
            assertEquals("no-store", result.getHeader("Cache-Control"),path);
            assertEquals("UTF-8", result.getCharacterEncoding(),path);
            String canonical = path.replaceFirst("^/next(?=/|$)","");
            if (List.of("","/","/home","/index.html").contains(canonical)) canonical = "/";
            assertTrue(html.contains("rel=\"canonical\" href=\"https://wydlgf.cn"+canonical+"\""),path);
            boolean personal = canonical.startsWith("/account") || canonical.equals("/records") || canonical.startsWith("/record/");
            String policy = personal ? "noindex,nofollow" : "noindex,follow";
            assertEquals(policy, result.getHeader("X-Robots-Tag"),path);
            assertTrue(html.contains("name=\"robots\" content=\""+policy+"\""),path);
            assertFalse(html.contains("private-token"),path);
            if (path.endsWith("-en")) assertTrue(html.contains("<html lang=\"en\">"),path);
        }
    }
    @Test void canonicalForwardingDoesNotNoindexPublicPagesButKeepsPrivateDocumentsOut() throws Exception {
        var preview = new FrontendNextPreviewController();
        for (String path : List.of("/", "/services", "/guides", "/rules-en", "/records", "/record/Q1", "/account/settings")) {
            var request = new MockHttpServletRequest("GET", "/next"+(path.equals("/")?"":path));
            request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI,path);
            var response = preview.frontendNext(request);
            boolean personal = path.startsWith("/account") || path.startsWith("/record");
            assertEquals(personal ? "noindex,nofollow" : null,response.getHeaders().getFirst("X-Robots-Tag"),path);
            assertEquals(personal,response.getBody().contains("name=\"robots\""),path);
            assertTrue(response.getBody().contains("rel=\"canonical\" href=\"https://wydlgf.cn"+path+"\""),path);
        }
    }
}
