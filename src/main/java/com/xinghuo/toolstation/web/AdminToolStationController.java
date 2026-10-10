package com.xinghuo.toolstation.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.toolstation.application.ToolStationApplicationService;
import com.xinghuo.upload.infrastructure.UploadService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin/tool-station"})
public class AdminToolStationController {
    private final ToolStationApplicationService toolStation;
    private final ToolStationWebMapper toolStationMapper;
    private final UploadService uploads;

    @PostMapping(value={"/image"})
    ApiDtos.SiteImageUploadResponse uploadToolStationImage(@RequestParam(value="file") MultipartFile file) {
        UploadAsset asset = this.uploads.storeImage(file, UploadAsset.Kind.TOOL_STATION_COVER);
        return new ApiDtos.SiteImageUploadResponse("/api/site-assets/" + asset.getId());
    }

    @GetMapping
    JsonNode toolStation() {
        return this.toolStationMapper.toResponse(this.toolStation.publicData());
    }

    @PutMapping
    JsonNode saveToolStation(@RequestBody JsonNode data) {
        return this.toolStationMapper.toResponse(
                this.toolStation.save(this.toolStationMapper.toJson(data)));
    }

    @PostMapping(value={"/reset"})
    JsonNode resetToolStation() {
        return this.toolStationMapper.toResponse(this.toolStation.reset());
    }

    @Generated
    public AdminToolStationController(ToolStationApplicationService toolStation, ToolStationWebMapper toolStationMapper, UploadService uploads) {
        this.toolStation = toolStation;
        this.toolStationMapper = toolStationMapper;
        this.uploads = uploads;
    }
}
