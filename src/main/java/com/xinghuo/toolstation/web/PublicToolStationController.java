package com.xinghuo.toolstation.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.toolstation.application.ToolStationApplicationService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/tool-station"})
public class PublicToolStationController {
    private final ToolStationApplicationService toolStation;
    private final ToolStationWebMapper toolStationMapper;

    @GetMapping
    JsonNode toolStation() {
        return this.toolStationMapper.toResponse(this.toolStation.publicData());
    }

    @Generated
    public PublicToolStationController(ToolStationApplicationService toolStation, ToolStationWebMapper toolStationMapper) {
        this.toolStation = toolStation;
        this.toolStationMapper = toolStationMapper;
    }
}
