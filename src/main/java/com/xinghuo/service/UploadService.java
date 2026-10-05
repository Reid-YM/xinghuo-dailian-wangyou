package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.UploadAssetRepository;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
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
            throw new BadRequestException("\u8bf7\u9009\u62e9\u56fe\u7247");
        }
        try {
            // 先只读图片头取尺寸，再决定是否解码。
            //
            // 原实现先 ImageIO.read 解码、之后才检查尺寸，这是可利用的：
            // 一个几 MB 的「解压炸弹」PNG（尺寸极大但压缩后很小）在解码阶段
            // 会申请数 GB 的 BufferedImage，而本站 JVM 堆上限只有 512MB，
            // 足以让匿名攻击者通过公开的 /api/uploads/quote 打崩站点。
            // ImageReader 的 getWidth/getHeight 只解析元数据，不分配像素缓冲。
            int width;
            int height;
            try (InputStream probe = file.getInputStream();
                 ImageInputStream iis = ImageIO.createImageInputStream(probe)) {
                if (iis == null) {
                    throw new BadRequestException("\u4ec5\u5141\u8bb8\u53ef\u89e3\u6790\u7684 JPG/PNG \u7b49\u56fe\u7247");
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (!readers.hasNext()) {
                    throw new BadRequestException("\u4ec5\u5141\u8bb8\u53ef\u89e3\u6790\u7684 JPG/PNG \u7b49\u56fe\u7247");
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
                throw new BadRequestException("\u4ec5\u5141\u8bb8\u53ef\u89e3\u6790\u7684 JPG/PNG \u7b49\u56fe\u7247");
            }
            if (width > MAX_DIMENSION || height > MAX_DIMENSION
                    || (long) width * height > MAX_PIXELS) {
                throw new BadRequestException("\u56fe\u7247\u50cf\u7d20\u5c3a\u5bf8\u8fc7\u5927");
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BadRequestException("\u4ec5\u5141\u8bb8\u53ef\u89e3\u6790\u7684 JPG/PNG \u7b49\u56fe\u7247");
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
        if (!EnumSet.of(UploadAsset.Kind.SITE_HERO, new UploadAsset.Kind[]{UploadAsset.Kind.SITE_BACKGROUND, UploadAsset.Kind.SITE_WECHAT_QR, UploadAsset.Kind.SERVICE_COVER, UploadAsset.Kind.GAME_COVER, UploadAsset.Kind.GAME_LOGO, UploadAsset.Kind.TOOL_STATION_COVER, UploadAsset.Kind.USER_AVATAR}).contains(a.getKind())) {
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

