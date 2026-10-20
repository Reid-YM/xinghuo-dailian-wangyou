package com.xinghuo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.xinghuo.config.I18nDictionary;
import com.xinghuo.config.SmartLocaleResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(value=-2147483608)
public class HtmlI18nTranslationFilter
extends OncePerRequestFilter {
    private static final Set<String> TRANSLATABLE_ATTRIBUTES = Set.of("content", "title", "placeholder", "aria-label", "alt", "data-label", "data-title", "data-note", "data-placeholder");
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)(\\s*=\\s*)([\"'])(.*?)\\3", 32);
    private static final Pattern HTML_LANG = Pattern.compile("(<html\\b[^>]*\\blang\\s*=\\s*)([\"'])([^\"']*)(\\2)", 2);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Set<String> TRANSLATABLE_JSON_PATHS = Set.of("/api/site-config", "/api/services", "/api/catalog", "/api/tool-station", "/api/public/site-settings", "/api/public/cases");
    private final SmartLocaleResolver localeResolver;
    private final I18nDictionary dictionary;
    @Value("${app.frontend-next.default-enabled:false}")
    private boolean frontendNextEnabled;

    public HtmlI18nTranslationFilter(SmartLocaleResolver localeResolver, I18nDictionary dictionary) {
        this.localeResolver = localeResolver;
        this.dictionary = dictionary;
    }

    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (uri.startsWith("/admin") || uri.startsWith("/assets/") || uri.equals("/language")) {
            return true;
        }
        if (uri.startsWith("/api/")) {
            return !this.isTranslatableJson(uri);
        }
        return uri.indexOf(46) >= 0 && !uri.endsWith(".html");
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Locale locale = this.localeResolver.resolveLocale(request);
        this.localeResolver.persistIfAbsent(request, response, locale);
        response.setHeader("Content-Language", locale.toLanguageTag());
        response.addHeader("Vary", "Cookie");
        // Canonical English legal pages forward to a Next ResponseEntity when enabled.
        // Wrapping that outer request can consume the forwarded body (HTTP 200, zero bytes).
        // Keep the existing translator for explicit legacy=1 and global rollback.
        String path = request.getRequestURI();
        if (frontendNextEnabled
                && ("/rules-en".equals(path) || "/privacy-en".equals(path))
                && !"1".equals(request.getParameter("legacy"))
                && Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            chain.doFilter((ServletRequest) request, (ServletResponse) response);
            return;
        }
        if (!Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            chain.doFilter((ServletRequest)request, (ServletResponse)response);
            return;
        }
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter((ServletRequest)request, (ServletResponse)wrapper);
        String contentType = wrapper.getContentType();
        if (contentType == null) {
            wrapper.copyBodyToResponse();
            return;
        }
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        byte[] body = wrapper.getContentAsByteArray();
        if (body.length == 0) {
            wrapper.copyBodyToResponse();
            return;
        }
        Charset charset = this.responseCharset(contentType);
        if (normalizedType.contains("application/json")) {
            String json = new String(body, charset);
            String translatedJson = this.translateJson(json);
            this.writeJson(wrapper, charset, translatedJson, json);
            return;
        }
        if (!normalizedType.contains("text/html")) {
            wrapper.copyBodyToResponse();
            return;
        }
        String html = new String(body, charset);
        String translated = this.translateHtml(html);
        if (translated.equals(html)) {
            wrapper.copyBodyToResponse();
            return;
        }
        byte[] output = translated.getBytes(charset);
        wrapper.resetBuffer();
        wrapper.setContentLength(output.length);
        wrapper.getWriter().write(translated);
        wrapper.copyBodyToResponse();
    }

    private boolean isTranslatableJson(String uri) {
        return TRANSLATABLE_JSON_PATHS.contains(uri) || uri.startsWith("/api/public/game-status");
    }

    private void writeJson(ContentCachingResponseWrapper wrapper, Charset charset, String translatedJson, String originalJson) throws IOException {
        if (translatedJson.equals(originalJson)) {
            wrapper.copyBodyToResponse();
            return;
        }
        byte[] output = translatedJson.getBytes(charset);
        wrapper.resetBuffer();
        wrapper.setContentLength(output.length);
        wrapper.getOutputStream().write(output);
        wrapper.copyBodyToResponse();
    }

    private String translateHtml(String html) {
        StringBuilder out = new StringBuilder(html.length() + 256);
        int index = 0;
        String rawTag = null;
        boolean jsonLd = false;
        while (index < html.length()) {
            if (rawTag != null) {
                int close = html.toLowerCase(Locale.ROOT).indexOf("</" + rawTag, index);
                if (close < 0) {
                    out.append(html, index, html.length());
                    break;
                }
                String raw = html.substring(index, close);
                out.append("script".equals(rawTag) && jsonLd ? this.translateJson(raw) : raw);
                index = close;
                rawTag = null;
                jsonLd = false;
                continue;
            }
            int tagStart = html.indexOf(60, index);
            if (tagStart < 0) {
                this.appendText(out, html.substring(index), rawTag);
                break;
            }
            if (tagStart > index) {
                this.appendText(out, html.substring(index, tagStart), rawTag);
            }
            if (html.startsWith("<!--", tagStart)) {
                int commentEnd = html.indexOf("-->", tagStart + 4);
                if (commentEnd < 0) {
                    out.append(html, tagStart, html.length());
                    break;
                }
                out.append(html, tagStart, commentEnd + 3);
                index = commentEnd + 3;
                continue;
            }
            int tagEnd = this.findTagEnd(html, tagStart + 1);
            if (tagEnd < 0) {
                out.append(html.substring(tagStart));
                break;
            }
            String tag = html.substring(tagStart, tagEnd + 1);
            out.append(this.translateTag(tag));
            String lower = tag.toLowerCase(Locale.ROOT);
            if (lower.matches("<script\\b[^>]*>")) {
                rawTag = "script";
                jsonLd = lower.contains("application/ld+json");
            } else if (lower.matches("</script\\s*>")) {
                rawTag = null;
                jsonLd = false;
            } else if (lower.matches("<style\\b[^>]*>")) {
                rawTag = "style";
            } else if (lower.matches("</style\\s*>")) {
                rawTag = null;
            }
            index = tagEnd + 1;
        }
        return this.ensureEnglishLang(out.toString());
    }

    private String translateJson(String json) {
        try {
            JsonNode root = JSON_MAPPER.readTree(json);
            return JSON_MAPPER.writeValueAsString(this.translateJsonNode(root));
        }
        catch (Exception ignored) {
            return json;
        }
    }

    private JsonNode translateJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isTextual()) {
            return TextNode.valueOf((String)this.dictionary.translate(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode translated = JSON_MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                if ("inLanguage".equals(entry.getKey())) {
                    translated.put((String)entry.getKey(), "en");
                } else {
                    translated.set((String)entry.getKey(), this.translateJsonNode((JsonNode)entry.getValue()));
                }
            });
            return translated;
        }
        if (node.isArray()) {
            ArrayNode translated = JSON_MAPPER.createArrayNode();
            node.forEach(item -> translated.add(this.translateJsonNode(item)));
            return translated;
        }
        return node;
    }

    private void appendText(StringBuilder out, String text, String rawTag) {
        int start;
        if (rawTag != null || !StringUtils.hasText((String)text) || !this.dictionary.containsChinese(text)) {
            out.append(text);
            return;
        }
        int end = text.length();
        for (start = 0; start < end && Character.isWhitespace(text.charAt(start)); ++start) {
        }
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            --end;
        }
        String core = text.substring(start, end);
        String translated = this.dictionary.translate(core);
        out.append(text, 0, start).append(translated).append(text, end, text.length());
    }

    private String translateTag(String tag) {
        Matcher matcher = ATTRIBUTE.matcher(tag);
        StringBuffer result = new StringBuffer(tag.length() + 32);
        while (matcher.find()) {
            String translated;
            String name = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = matcher.group(4);
            if (!TRANSLATABLE_ATTRIBUTES.contains(name) || !this.dictionary.containsChinese(value) || (translated = this.dictionary.translate(value)).equals(value)) continue;
            String replacement = matcher.group(1) + matcher.group(2) + matcher.group(3) + translated + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String ensureEnglishLang(String html) {
        Matcher matcher = HTML_LANG.matcher(html);
        if (matcher.find()) {
            return matcher.replaceFirst("$1$2en$4");
        }
        return html.replaceFirst("(?i)<html\\b", "<html lang=\"en\"");
    }

    private int findTagEnd(String html, int start) {
        char quote = '\u0000';
        for (int i = start; i < html.length(); ++i) {
            char c = html.charAt(i);
            if (quote != '\u0000') {
                if (c != quote) continue;
                quote = '\u0000';
                continue;
            }
            if (c == '\'' || c == '\"') {
                quote = c;
                continue;
            }
            if (c != '>') continue;
            return i;
        }
        return -1;
    }

    private Charset responseCharset(String contentType) {
        for (String part : contentType.split(";")) {
            String value = part.trim();
            if (!value.toLowerCase(Locale.ROOT).startsWith("charset=")) continue;
            try {
                return Charset.forName(value.substring("charset=".length()).trim());
            }
            catch (Exception ignored) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }
}

