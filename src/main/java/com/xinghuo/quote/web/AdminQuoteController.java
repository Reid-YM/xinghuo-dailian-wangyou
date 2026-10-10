package com.xinghuo.quote.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.quote.application.QuoteApplicationService;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin/quotes"})
public class AdminQuoteController {
    private final QuoteApplicationService quotes;
    private final UploadService uploads;

    @GetMapping
    List<ApiDtos.QuoteAdminResponse> quotes() {
        return QuoteWebMapper.toAdminResponses(this.quotes.list());
    }

    @PatchMapping(value={"/{id}/status"})
    ApiDtos.QuoteAdminResponse quoteStatus(@PathVariable UUID id, @Valid @RequestBody ApiDtos.QuoteStatusUpdate r, Authentication a, HttpServletRequest req) {
        return QuoteWebMapper.toResponse(
                this.quotes.updateStatus(id, QuoteWebMapper.toCommand(r), a.getName()));
    }

    @DeleteMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteQuote(@PathVariable UUID id, Authentication a, HttpServletRequest req) {
        this.quotes.delete(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value={"/{id}/convert"})
    ApiDtos.AdminOrderResponse convertQuote(@PathVariable UUID id, @Valid @RequestBody ApiDtos.QuoteConvertRequest r, Authentication a, HttpServletRequest req) {
        return QuoteWebMapper.toResponse(
                this.quotes.convertToOrder(id, QuoteWebMapper.toCommand(r), a.getName()));
    }

    @GetMapping(value={"/{id}/image"})
    ResponseEntity<Resource> quoteImage(@PathVariable UUID id) {
        var q = this.quotes.find(id);
        if (q.screenshotAssetId() == null) {
            throw new NotFoundException();
        }
        return ResponseEntity.ok()
                .contentType(this.uploads.mediaType(q.screenshotAssetId()))
                .cacheControl(CacheControl.noStore())
                .body(this.uploads.load(q.screenshotAssetId(), "QUOTE", q.id().toString()));
    }

    @Generated
    public AdminQuoteController(QuoteApplicationService quotes, UploadService uploads) {
        this.quotes = quotes;
        this.uploads = uploads;
    }
}
