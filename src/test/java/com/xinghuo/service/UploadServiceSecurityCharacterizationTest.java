package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.UploadAssetRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

class UploadServiceSecurityCharacterizationTest {

    @TempDir
    Path tempDir;

    @Test
    void nullBindIsNoOpWithoutRepositoryLookup() {
        UploadAssetRepository repo = mock(UploadAssetRepository.class);
        UploadService service = service(repo, tempDir.resolve("uploads"));

        service.bind(null, "ORDER", "XHABC1234567");

        verifyNoInteractions(repo);
    }

    @Test
    void bindRejectsAssetAlreadyOwnedByDifferentBusiness() {
        UUID id = UUID.randomUUID();
        UploadAssetRepository repo = mock(UploadAssetRepository.class);
        UploadAsset asset = UploadAsset.builder()
                .id(id)
                .kind(UploadAsset.Kind.ORDER_PROGRESS)
                .storedName("order_progress/example.jpg")
                .contentType("image/jpeg")
                .ownerType("ORDER")
                .ownerRef("XHOLD1234567")
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(asset));
        UploadService service = service(repo, tempDir.resolve("uploads"));

        assertThrows(ConflictException.class,
                () -> service.bind(id, "ORDER", "XHNEW1234567"));

        assertEquals("ORDER", asset.getOwnerType());
        assertEquals("XHOLD1234567", asset.getOwnerRef());
    }

    @Test
    void privateLoadRequiresExactOwnerTypeAndReference() {
        UUID id = UUID.randomUUID();
        UploadAssetRepository repo = mock(UploadAssetRepository.class);
        UploadAsset asset = UploadAsset.builder()
                .id(id)
                .kind(UploadAsset.Kind.ORDER_PROGRESS)
                .storedName("order_progress/example.jpg")
                .contentType("image/jpeg")
                .ownerType("ORDER")
                .ownerRef("XHABC1234567")
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(asset));
        UploadService service = service(repo, tempDir.resolve("uploads"));

        assertThrows(NotFoundException.class,
                () -> service.load(id, "QUOTE", "XHABC1234567"));
        assertThrows(NotFoundException.class,
                () -> service.load(id, "ORDER", "XHOTHER12345"));
    }

    @Test
    void publicLoaderRejectsPrivateQuoteScreenshotEvenWhenFileExists() throws Exception {
        UUID id = UUID.randomUUID();
        Path uploadDir = tempDir.resolve("uploads");
        Path file = uploadDir.resolve("quote_screenshot/private.png");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "private");

        UploadAssetRepository repo = mock(UploadAssetRepository.class);
        UploadAsset asset = UploadAsset.builder()
                .id(id)
                .kind(UploadAsset.Kind.QUOTE_SCREENSHOT)
                .storedName("quote_screenshot/private.png")
                .contentType("image/png")
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(asset));
        UploadService service = service(repo, uploadDir);

        assertThrows(NotFoundException.class, () -> service.loadPublicSiteAsset(id));
    }

    @Test
    void publicLoaderAllowsWhitelistedAvatarWhenFileIsInsideUploadRoot() throws Exception {
        UUID id = UUID.randomUUID();
        Path uploadDir = tempDir.resolve("uploads");
        Path file = uploadDir.resolve("user_avatar/avatar.png");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "avatar");

        UploadAssetRepository repo = mock(UploadAssetRepository.class);
        UploadAsset asset = UploadAsset.builder()
                .id(id)
                .kind(UploadAsset.Kind.USER_AVATAR)
                .storedName("user_avatar/avatar.png")
                .contentType("image/png")
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(asset));
        UploadService service = service(repo, uploadDir);

        Resource resource = service.loadPublicSiteAsset(id);

        assertTrue(resource.exists());
        assertEquals(file.normalize(), resource.getFile().toPath().normalize());
    }

    @Test
    void publicLoaderRejectsNormalizedPathEscapingUploadRootEvenIfFileExists() throws Exception {
        UUID id = UUID.randomUUID();
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        Path outside = tempDir.resolve("outside.png");
        Files.writeString(outside, "outside");

        UploadAssetRepository repo = mock(UploadAssetRepository.class);
        UploadAsset asset = UploadAsset.builder()
                .id(id)
                .kind(UploadAsset.Kind.SITE_HERO)
                .storedName("../outside.png")
                .contentType("image/png")
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(asset));
        UploadService service = service(repo, uploadDir);

        assertThrows(NotFoundException.class, () -> service.loadPublicSiteAsset(id));
    }

    private static UploadService service(UploadAssetRepository repo, Path uploadDir) {
        UploadService service = new UploadService(repo);
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.toString());
        return service;
    }
}
