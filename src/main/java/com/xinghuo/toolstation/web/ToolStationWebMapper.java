package com.xinghuo.toolstation.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import org.springframework.stereotype.Component;

/** Maps the application-owned JSON text to and from established HTTP JsonNode shapes. */
@Component
public class ToolStationWebMapper {
    private final ObjectMapper mapper;

    public ToolStationWebMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String toJson(JsonNode data) {
        return data == null ? null : data.toString();
    }

    public JsonNode toResponse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new BadRequestException("工具站数据读取失败");
        }
    }
}
