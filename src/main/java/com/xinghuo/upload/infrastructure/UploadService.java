package com.xinghuo.upload.infrastructure;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.UploadAssetRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Shared upload infrastructure service. */
@Service
public class UploadService {
    /** 单边像素上限。 */
    private static final int MAX_DIMENSION = 12000;
    /** 总像素上限（约 4000 万），防止两边都不超限但乘积极大的图。 */
    private static final long MAX_PIXELS = 40_000_000L;
    private final UploadAssetRepository repo;
    @Value(value="${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public UploadAsset storeImage(MultipartFile file, UploadAsset.Kind kind) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择图片");
        }
        try {
            int width;
            int height;
            try (InputStream probe = file.getInputStream();
                 ImageInputStream iis = ImageIO.createImageInputStream(probe)) {
                if (iis == null) {
                    throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (!readers.hasNext()) {
                    throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                } finally {
                    reader.dispose();
                }
            }
            if (width <= 0 || height <= 0) {
                throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
            }
            if (width > MAX_DIMENSION || height > MAX_DIMENSION
                    || (long) width * height > MAX_PIXELS) {
                throw new BadRequestException("图片像素尺寸过大");
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
            }
            UUID id = UUID.randomUUID();
            Path dir = Paths.get(this.uploadDir, kind.name().toLowerCase());
            Files.createDirectories(dir);
            String format = image.getColorModel().hasAlpha() ? "png" : "jpg";
            String stored = id + "." + format;
            Path dest = dir.resolve(stored).normalize();
            if (!dest.startsWith(dir.normalize())) {
                throw new BadRequestException("非法文件路径");
            }
            if (!ImageIO.write(image, format, dest.toFile())) {
                throw new IOException("Image encode failed");
            }
            long size = Files.size(dest);
            UploadAsset a = UploadAsset.builder()
                    .id(id)
                    .kind(kind)
                    .storedName(kind.name().toLowerCase() + "/" + stored)
                    .originalName(this.safeName(file.getOriginalFilename()))
                    .contentType("png".equals(format) ? "image/png" : "image/jpeg")
                    .byteSize(size)
                    .createdAt(Instant.now())
                    .build();
            return this.repo.save(a);
        }
        catch (BadRequestException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BadRequestException("图片处理失败");
        }
    }

    public org.springframework.http.MediaType mediaType(UUID assetId) {
        UploadAsset asset = repo.findById(assetId).orElseThrow(NotFoundException::new);
        return "image/png".equals(asset.getContentType())
                ? org.springframework.http.MediaType.IMAGE_PNG : org.springframework.http.MediaType.IMAGE_JPEG;
    }

    @Transactional
    public void bind(UUID assetId, String ownerType, String ownerRef) {
        if (assetId == null) {
            return;
        }
        UploadAsset a = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (a.getOwnerRef() != null && !Objects.equals(a.getOwnerRef(), ownerRef)) {
            throw new ConflictException("图片已被其他业务占用");
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
        if (!EnumSet.of(
                UploadAsset.Kind.SITE_HERO,
                UploadAsset.Kind.SITE_BACKGROUND,
                UploadAsset.Kind.SITE_WECHAT_QR,
                UploadAsset.Kind.SERVICE_COVER,
                UploadAsset.Kind.GAME_COVER,
                UploadAsset.Kind.GAME_LOGO,
                UploadAsset.Kind.TOOL_STATION_COVER,
                UploadAsset.Kind.USER_AVATAR).contains(a.getKind())) {
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
        return Paths.get(s).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Generated
    public UploadService(UploadAssetRepository repo) {
        this.repo = repo;
    }
}
