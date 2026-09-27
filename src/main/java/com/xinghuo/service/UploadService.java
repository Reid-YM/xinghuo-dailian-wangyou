/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.exception.ConflictException
 *  com.xinghuo.exception.NotFoundException
 *  com.xinghuo.model.UploadAsset
 *  com.xinghuo.model.UploadAsset$Kind
 *  com.xinghuo.repository.UploadAssetRepository
 *  com.xinghuo.service.UploadService
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.io.FileSystemResource
 *  org.springframework.core.io.Resource
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.multipart.MultipartFile
 */
package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.UploadAssetRepository;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {
    private final UploadAssetRepository repo;
    @Value(value="${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public UploadAsset storeImage(MultipartFile file, UploadAsset.Kind kind) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("\u8bf7\u9009\u62e9\u56fe\u7247");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BadRequestException("\u4ec5\u5141\u8bb8\u53ef\u89e3\u6790\u7684 JPG/PNG \u7b49\u56fe\u7247");
            }
            if (image.getWidth() > 12000 || image.getHeight() > 12000) {
                throw new BadRequestException("\u56fe\u7247\u50cf\u7d20\u5c3a\u5bf8\u8fc7\u5927");
            }
            UUID id = UUID.randomUUID();
            Path dir = Paths.get(this.uploadDir, kind.name().toLowerCase());
            Files.createDirectories(dir);
            String stored = String.valueOf(id) + ".jpg";
            Path dest = dir.resolve(stored).normalize();
            if (!dest.startsWith(dir.normalize())) {
                throw new BadRequestException("\u975e\u6cd5\u6587\u4ef6\u8def\u5f84");
            }
            if (!ImageIO.write(image, "jpg", dest.toFile())) {
                throw new IOException("Image encode failed");
            }
            long size = Files.size(dest);
            UploadAsset a = UploadAsset.builder().id(id).kind(kind).storedName(kind.name().toLowerCase() + "/" + stored).originalName(this.safeName(file.getOriginalFilename())).contentType("image/jpeg").byteSize(size).createdAt(Instant.now()).build();
            return this.repo.save(a);
        }
        catch (BadRequestException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BadRequestException("\u56fe\u7247\u5904\u7406\u5931\u8d25");
        }
    }

    @Transactional
    public void bind(UUID assetId, String ownerType, String ownerRef) {
        if (assetId == null) {
            return;
        }
        UploadAsset a = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (a.getOwnerRef() != null && !Objects.equals(a.getOwnerRef(), ownerRef)) {
            throw new ConflictException("\u56fe\u7247\u5df2\u88ab\u5176\u4ed6\u4e1a\u52a1\u5360\u7528");
        }
        a.setOwnerType(ownerType);
        a.setOwnerRef(ownerRef);
    }

    @Transactional(readOnly=true)
    public Resource load(UUID assetId, String ownerType, String ownerRef) {
        Path base;
        UploadAsset a = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (!Objects.equals(ownerType, a.getOwnerType()) || !Objects.equals(ownerRef, a.getOwnerRef())) {
            throw new NotFoundException();
        }
        Path p = Paths.get(this.uploadDir).resolve(a.getStoredName()).normalize();
        if (!p.startsWith(base = Paths.get(this.uploadDir).normalize()) || !Files.isRegularFile(p)) {
            throw new NotFoundException();
        }
        return new FileSystemResource(p);
    }

    @Transactional(readOnly=true)
    public Resource loadPublicSiteAsset(UUID assetId) {
        Path base;
        UploadAsset a = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (!EnumSet.of(UploadAsset.Kind.SITE_HERO, new UploadAsset.Kind[]{UploadAsset.Kind.SITE_BACKGROUND, UploadAsset.Kind.SITE_WECHAT_QR, UploadAsset.Kind.SERVICE_COVER, UploadAsset.Kind.GAME_COVER, UploadAsset.Kind.GAME_LOGO, UploadAsset.Kind.TOOL_STATION_COVER}).contains(a.getKind())) {
            throw new NotFoundException();
        }
        Path p = Paths.get(this.uploadDir).resolve(a.getStoredName()).normalize();
        if (!p.startsWith(base = Paths.get(this.uploadDir).normalize()) || !Files.isRegularFile(p)) {
            throw new NotFoundException();
        }
        return new FileSystemResource(p);
    }

    private String safeName(String s) {
        if (s == null) {
            return null;
        }
        return Paths.get(s, new String[0]).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Generated
    public UploadService(UploadAssetRepository repo) {
        this.repo = repo;
    }
}

