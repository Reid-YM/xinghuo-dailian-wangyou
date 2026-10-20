package com.xinghuo.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

class HtmlI18nNextForwardRegressionTest {
    private HtmlI18nTranslationFilter filter(boolean nextEnabled) {
        SmartLocaleResolver locale = mock(SmartLocaleResolver.class);
        when(locale.resolveLocale(any())).thenReturn(Locale.ENGLISH);
        HtmlI18nTranslationFilter filter = new HtmlI18nTranslationFilter(locale, mock(I18nDictionary.class));
        ReflectionTestUtils.setField(filter, "frontendNextEnabled", nextEnabled);
        return filter;
    }

    private void verifyResponse(String path, boolean nextEnabled, boolean legacy, boolean expectedWrapped)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (legacy) request.setParameter("legacy", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean wrapped = new AtomicBoolean();
        String document = "<html lang=\"en\"><script src=\"/next/shell.js?v=abc\"></script></html>";
        filter(nextEnabled).doFilter(request, response, (req, res) -> {
            wrapped.set(res instanceof ContentCachingResponseWrapper);
            res.setContentType("text/html;charset=UTF-8");
            res.getWriter().write(document);
            res.getWriter().flush();
        });

        assertEquals(expectedWrapped, wrapped.get(), path + " legacy=" + legacy);
        assertEquals(document, response.getContentAsString(), path + " body must survive");
        assertEquals("en", response.getHeader("Content-Language"));
    }

    @Test void englishCanonicalNextForwardUsesUnwrappedResponseAndPreservesBody() throws Exception {
        verifyResponse("/rules-en", true, false, false);
        verifyResponse("/privacy-en", true, false, false);
    }

    @Test void legacyAndGlobalRollbackStillApplyTheOriginalTranslationFilter() throws Exception {
        verifyResponse("/rules-en", true, true, false);
        verifyResponse("/privacy-en", true, true, false);
        verifyResponse("/rules-en", false, false, true);
        verifyResponse("/privacy-en", false, false, true);
    }
}
