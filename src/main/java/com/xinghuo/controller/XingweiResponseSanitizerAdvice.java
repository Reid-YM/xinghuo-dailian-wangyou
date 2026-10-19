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

/** Hides internal business notes at the native workspace's JSON boundary, without changing storage. */
@RestControllerAdvice
public class XingweiResponseSanitizerAdvice implements ResponseBodyAdvice<Object> {
    private static final String ROOT = "/api/admin/xingwei";
    private static final Set<String> PRIVATE_NOTES = Set.of(
            "internalNote", "internal_note", "handlingNote", "handling_note", "operatorNote", "operator_note", "followUpNote", "follow_up_note");
    private final ObjectMapper mapper;

    public XingweiResponseSanitizerAdvice(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean supports(MethodParameter type, Class<? extends HttpMessageConverter<?>> converter) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converter);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter type, MediaType media,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request,
            ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (body == null || !(path.equals(ROOT) || path.startsWith(ROOT + "/"))) return body;
        JsonNode tree = mapper.valueToTree(body);
        strip(tree, path.equals(ROOT + "/release-confirmation"));
        return tree;
    }

    private void strip(JsonNode node, boolean hideConfirmationNotes) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            PRIVATE_NOTES.forEach(object::remove);
            if (hideConfirmationNotes || (object.has("channelCode") && object.has("serviceState"))) object.remove("note");
            object.elements().forEachRemaining(child -> strip(child, hideConfirmationNotes));
        } else if (node.isArray()) node.elements().forEachRemaining(child -> strip(child, hideConfirmationNotes));
    }
}
