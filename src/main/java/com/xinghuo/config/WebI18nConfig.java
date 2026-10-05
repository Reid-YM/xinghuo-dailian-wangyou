package com.xinghuo.config;

import com.xinghuo.config.SmartLocaleResolver;
import com.xinghuo.security.GeoLanguageResolver;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class WebI18nConfig
implements WebMvcConfigurer {
    public static final List<Locale> SUPPORTED = List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;
    public static final String COOKIE_NAME = "xh_lang";
    public static final String PARAM_NAME = "lang";

    @Bean
    MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames(new String[]{"classpath:messages"});
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Bean
    SmartLocaleResolver localeResolver(GeoLanguageResolver geo) {
        return new SmartLocaleResolver(geo);
    }

    @Bean
    LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName(PARAM_NAME);
        return interceptor;
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor((HandlerInterceptor)this.localeChangeInterceptor());
    }

    public static Locale normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (v.startsWith("zh")) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if (v.startsWith("en")) {
            return Locale.ENGLISH;
        }
        for (Locale locale : SUPPORTED) {
            if (!locale.getLanguage().equalsIgnoreCase(v)) continue;
            return locale;
        }
        return DEFAULT_LOCALE;
    }
}

