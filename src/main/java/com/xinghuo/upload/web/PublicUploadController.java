package com.xinghuo.upload.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.upload.application.UploadApplicationService;
import com.xinghuo.upload.application.UploadPayload;
import com.xinghuo.upload.application.UploadStored;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import lombok.Generated;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api"})
public class PublicUploadController {
    private final UploadApplicationService uploads;

    @GetMapping(value={"/site-assets/{assetId}"})
    ResponseEntity<byte[]> siteAsset(@PathVariable UUID assetId) {
        UploadPayload payload = this.uploads.loadPublicSiteAsset(assetId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.mediaType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7L)).cachePublic())
                .body(payload.content());
    }

    @PostMapping(value={"/uploads/quote"})
    ApiDtos.UploadResponse quoteUpload(@RequestParam(value="file") MultipartFile file) {
        try {
            UploadStored stored = this.uploads.storeQuoteScreenshot(file.getBytes(), file.getOriginalFilename());
            return new ApiDtos.UploadResponse(stored.id());
        }
        catch (IOException e) {
            throw new BadRequestException("图片处理失败: " + safeMessage(e));
        }
    }

    private static String safeMessage(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            return t.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    @Generated
    public PublicUploadController(UploadApplicationService uploads) {
        this.uploads = uploads;
    }
}
