package com.xinghuo.quote.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.upload.infrastructure.UploadService;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/uploads"})
public class PublicQuoteUploadController {
    private final UploadService uploads;

    @PostMapping(value={"/quote"})
    ApiDtos.UploadResponse quoteUpload(@RequestParam(value="file") MultipartFile file) {
        return new ApiDtos.UploadResponse(
                this.uploads.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT).getId());
    }

    @Generated
    public PublicQuoteUploadController(UploadService uploads) {
        this.uploads = uploads;
    }
}
