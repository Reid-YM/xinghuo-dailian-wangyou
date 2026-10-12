package com.xinghuo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.security.GeoLanguageResolver;
import jakarta.servlet.http.Cookie;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SmartLocaleResolverSeoTest {

    @Test
    void chineseLegalCanonicalPathsStayChineseDespiteEnglishSignals() {
        GeoLanguageResolver geo = mock(GeoLanguageResolver.class);
        when(geo.detectLanguage(anyString())).thenReturn("en");
        SmartLocaleResolver resolver = new SmartLocaleResolver(geo);

        MockHttpServletRequest rules = new MockHttpServletRequest("GET", "/rules");
        rules.setRemoteAddr("8.8.8.8");
        rules.addHeader("Accept-Language", "en-US,en;q=0.9");
        rules.setCookies(new Cookie("xh_lang", "en"));

        MockHttpServletRequest privacy = new MockHttpServletRequest("GET", "/privacy");
        privacy.setRemoteAddr("8.8.8.8");
        privacy.addHeader("Accept-Language", "en-US,en;q=0.9");
        privacy.setParameter("lang", "en");

        assertEquals(Locale.SIMPLIFIED_CHINESE, resolver.resolveLocale(rules));
        assertEquals(Locale.SIMPLIFIED_CHINESE, resolver.resolveLocale(privacy));
    }

    @Test
    void englishLegalCanonicalPathsStayEnglishDespiteChineseSignals() {
        GeoLanguageResolver geo = mock(GeoLanguageResolver.class);
        when(geo.detectLanguage(anyString())).thenReturn("zh");
        SmartLocaleResolver resolver = new SmartLocaleResolver(geo);

        MockHttpServletRequest rules = new MockHttpServletRequest("GET", "/rules-en");
        rules.setRemoteAddr("1.1.1.1");
        rules.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        rules.setCookies(new Cookie("xh_lang", "zh"));

        MockHttpServletRequest privacy = new MockHttpServletRequest("GET", "/privacy-en");
        privacy.setRemoteAddr("1.1.1.1");
        privacy.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        privacy.setParameter("lang", "zh");

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(rules));
        assertEquals(Locale.ENGLISH, resolver.resolveLocale(privacy));
    }
}
