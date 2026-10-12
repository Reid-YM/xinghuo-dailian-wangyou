package com.xinghuo.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {
    /** 单边像素上限。 */
    private static final int MAX_DIMENSION = 12_000;
    /** 总像素上限（约 4000 万），防止两边都不超限但乘积极大的图。 */
    private static final long MAX_PIXELS = 40_000_000L;
    private static final EnumSet<UploadAsset.Kind> PUBLIC_SITE_KINDS = EnumSet.of(
            UploadAsset.Kind.SITE_HERO,
            UploadAsset.Kind.SITE_BACKGROUND,
            UploadAsset.Kind.SITE_WECHAT_QR,
            UploadAsset.Kind.SERVICE_COVER,
            UploadAsset.Kind.GAME_COVER,
            UploadAsset.Kind.GAME_LOGO,
            UploadAsset.Kind.TOOL_STATION_COVER,
            UploadAsset.Kind.USER_AVATAR);

    private final UploadAssetRepository repo;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public UploadService(UploadAssetRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UploadAsset storeImage(MultipartFile file, UploadAsset.Kind kind) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择图片");
        }
        try {
            // 先只读图片头取尺寸，再决定是否解码。
            // 原实现先 ImageIO.read 解码、之后才检查尺寸，一个压缩后很小但像素极大的
            // PNG 可能在解码阶段占用大量堆内存。ImageReader 的宽高读取只解析元数据。
            int width;
            int height;
            try (InputStream probe = file.getInputStream();
                 ImageInputStream imageInput = ImageIO.createImageInputStream(probe)) {
                if (imageInput == null) {
                    throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) {
                    throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                }
                finally {
                    reader.dispose();
                }
            }

            if (width <= 0 || height <= 0) {
                throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
            }
            if (width > MAX_DIMENSION
                    || height > MAX_DIMENSION
                    || (long) width * height > MAX_PIXELS) {
                throw new BadRequestException("图片像素尺寸过大");
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BadRequestException("仅允许可解析的 JPG/PNG 等图片");
            }

            UUID id = UUID.randomUUID();
            String kindDir = kind.name().toLowerCase();
            Path dir = Paths.get(this.uploadDir, kindDir);
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
            UploadAsset asset = UploadAsset.builder()
                    .id(id)
                    .kind(kind)
                    .storedName(kindDir + "/" + stored)
                    .originalName(this.safeName(file.getOriginalFilename()))
                    .contentType("png".equals(format) ? "image/png" : "image/jpeg")
                    .byteSize(size)
                    .createdAt(Instant.now())
                    .build();
            return this.repo.save(asset);
        }
        catch (BadRequestException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BadRequestException("图片处理失败");
        }
    }

    public MediaType mediaType(UUID assetId) {
        UploadAsset asset = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        return "image/png".equals(asset.getContentType()) ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    @Transactional
    public void bind(UUID assetId, String ownerType, String ownerRef) {
        if (assetId == null) {
            return;
        }
        UploadAsset asset = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (asset.getOwnerRef() != null && !Objects.equals(asset.getOwnerRef(), ownerRef)) {
            throw new ConflictException("图片已被其他业务占用");
        }
        asset.setOwnerType(ownerType);
        asset.setOwnerRef(ownerRef);
    }

    @Transactional(readOnly = true)
    public Resource load(UUID assetId, String ownerType, String ownerRef) {
        UploadAsset asset = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (!Objects.equals(ownerType, asset.getOwnerType())
                || !Objects.equals(ownerRef, asset.getOwnerRef())) {
            throw new NotFoundException();
        }

        Path base = Paths.get(this.uploadDir).normalize();
        Path path = Paths.get(this.uploadDir).resolve(asset.getStoredName()).normalize();
        if (!path.startsWith(base) || !Files.isRegularFile(path)) {
            throw new NotFoundException();
        }
        return new FileSystemResource(path);
    }

    @Transactional(readOnly = true)
    public Resource loadPublicSiteAsset(UUID assetId) {
        UploadAsset asset = this.repo.findById(assetId).orElseThrow(NotFoundException::new);
        if (!PUBLIC_SITE_KINDS.contains(asset.getKind())) {
            throw new NotFoundException();
        }

        Path base = Paths.get(this.uploadDir).normalize();
        Path path = Paths.get(this.uploadDir).resolve(asset.getStoredName()).normalize();
        if (!path.startsWith(base) || !Files.isRegularFile(path)) {
            throw new NotFoundException();
        }
        return new FileSystemResource(path);
    }

    private String safeName(String value) {
        if (value == null) {
            return null;
        }
        return Paths.get(value).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
