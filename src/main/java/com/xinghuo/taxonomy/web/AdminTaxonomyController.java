package com.xinghuo.taxonomy.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin/catalog"})
public class AdminTaxonomyController {
    private final TaxonomyApplicationService taxonomy;
    private final UploadService uploads;

    @GetMapping
    ApiDtos.CatalogResponse catalog() {
        return TaxonomyWebMapper.toResponse(this.taxonomy.catalog(true));
    }

    @PostMapping(value={"/games"})
    ApiDtos.GameOptionResponse addGame(@Valid @RequestBody ApiDtos.CatalogNameRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.addGame(TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/categories"})
    ApiDtos.CategoryOptionResponse addCategory(@Valid @RequestBody ApiDtos.CategoryCreateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.addCategory(TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/games/{code}"})
    ApiDtos.GameOptionResponse updateGame(@PathVariable String code, @Valid @RequestBody ApiDtos.CatalogUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.updateGame(code, TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/games/{code}/profile"})
    ApiDtos.GameOptionResponse updateGameProfile(@PathVariable String code, @Valid @RequestBody ApiDtos.GameProfileUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.updateGameProfile(code, TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PatchMapping(value={"/games/bulk"})
    List<ApiDtos.GameOptionResponse> bulkUpdateGames(@Valid @RequestBody ApiDtos.GameBulkUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toGameResponses(
                this.taxonomy.bulkUpdateGames(TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/games/image/{kind}"})
    ApiDtos.SiteImageUploadResponse uploadGameImage(@PathVariable String kind, @RequestParam(value="file") MultipartFile file) {
        UploadAsset.Kind imageKind = switch (kind.trim().toUpperCase(Locale.ROOT)) {
            case "LOGO" -> UploadAsset.Kind.GAME_LOGO;
            case "COVER" -> UploadAsset.Kind.GAME_COVER;
            default -> throw new BadRequestException("非法游戏图片类型");
        };
        UploadAsset asset = this.uploads.storeImage(file, imageKind);
        return new ApiDtos.SiteImageUploadResponse("/api/site-assets/" + asset.getId());
    }

    @DeleteMapping(value={"/games/{code}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteGame(@PathVariable String code, Authentication a, HttpServletRequest req) {
        this.taxonomy.deleteGame(code, a.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value={"/categories/{id}"})
    ApiDtos.CategoryOptionResponse updateCategory(@PathVariable Long id, @Valid @RequestBody ApiDtos.CategoryUpdateRequest r, Authentication a, HttpServletRequest req) {
        return TaxonomyWebMapper.toResponse(
                this.taxonomy.updateCategory(id, TaxonomyWebMapper.toCommand(r), a.getName()));
    }

    @DeleteMapping(value={"/categories/{id}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication a, HttpServletRequest req) {
        this.taxonomy.deleteCategory(id, a.getName());
        return ResponseEntity.noContent().build();
    }

    @Generated
    public AdminTaxonomyController(TaxonomyApplicationService taxonomy, UploadService uploads) {
        this.taxonomy = taxonomy;
        this.uploads = uploads;
    }
}
