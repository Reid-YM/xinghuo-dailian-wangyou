package com.xinghuo.catalog.web;

import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin/services"})
public class AdminCatalogServiceController {
    private final CatalogApplicationService services;
    private final UploadService uploads;

    @PostMapping(value={"/cover-image"})
    ApiDtos.SiteImageUploadResponse uploadServiceCover(@RequestParam(value="file") MultipartFile file) {
        UploadAsset asset = this.uploads.storeImage(file, UploadAsset.Kind.SERVICE_COVER);
        return new ApiDtos.SiteImageUploadResponse("/api/site-assets/" + asset.getId());
    }

    @GetMapping
    List<ApiDtos.AdminServiceResponse> listServices() {
        return CatalogAdminWebMapper.toResponses(this.services.adminList());
    }

    @PostMapping
    ApiDtos.AdminServiceResponse createService(@Valid @RequestBody ApiDtos.ServiceUpsert r, Authentication a, HttpServletRequest req) {
        return CatalogAdminWebMapper.toResponse(this.services.create(CatalogAdminWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/{id}"})
    ApiDtos.AdminServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ApiDtos.ServiceUpsert r, Authentication a, HttpServletRequest req) {
        return CatalogAdminWebMapper.toResponse(this.services.update(id, CatalogAdminWebMapper.toCommand(r), a.getName()));
    }

    @DeleteMapping(value={"/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteService(@PathVariable Long id, Authentication a, HttpServletRequest req) {
        this.services.delete(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value={"/{id}/restore"})
    ApiDtos.AdminServiceResponse restoreService(@PathVariable Long id, Authentication a, HttpServletRequest req) {
        return CatalogAdminWebMapper.toResponse(this.services.restore(id, a.getName()));
    }

    @Generated
    public AdminCatalogServiceController(CatalogApplicationService services, UploadService uploads) {
        this.services = services;
        this.uploads = uploads;
    }
}
