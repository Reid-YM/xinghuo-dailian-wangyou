package com.xinghuo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Keeps persistence/admin-only identifiers out of JSON consumed by public pages.
 *
 * <p>The underlying DTOs stay unchanged because the admin console legitimately needs
 * database ids and version/source metadata. Public pages should use stable public keys
 * such as slug, game/category code and business number instead.</p>
 */
@RestControllerAdvice(assignableTypes = PublicApiController.class)
public class PublicResponseSanitizerAdvice implements ResponseBodyAdvice<Object> {
    private static final Set<String> SANITIZED_PATHS = Set.of(
            "/api/site-config",
            "/api/services",
            "/api/catalog",
            "/api/quote"
    );

    private static final Set<String> INTERNAL_FIELDS = Set.of(
            "id",
            "internalNote",
            "internal_note", "handlingNote", "handling_note", "operatorNote", "operator_note", "followUpNote", "follow_up_note",
            "version",
            "sourceQuoteId",
            "customerIpHash"
    );

    private final ObjectMapper objectMapper;

    public PublicResponseSanitizerAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        String path = request.getURI().getPath();
        boolean privateLookup = path.matches("/api/(?:records|orders)/[^/]+");
        if (body == null || (!privateLookup && !SANITIZED_PATHS.contains(path))) {
            return body;
        }

        if (privateLookup) response.getHeaders().setCacheControl("no-store");
        JsonNode tree = objectMapper.valueToTree(body);
        stripInternalFields(tree);
        return tree;
    }

    private void stripInternalFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            INTERNAL_FIELDS.forEach(object::remove);
            object.elements().forEachRemaining(this::stripInternalFields);
            return;
        }
        if (node.isArray()) {
            node.elements().forEachRemaining(this::stripInternalFields);
        }
    }
}
