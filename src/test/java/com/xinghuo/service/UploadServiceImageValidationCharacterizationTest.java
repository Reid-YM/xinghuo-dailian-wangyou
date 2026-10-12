package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.UploadAssetRepository;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class UploadServiceImageValidationCharacterizationTest {

    @TempDir
    Path tempDir;

    @Test
    void emptyUploadIsRejectedBeforeImageParsing() {
        UploadService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT));

        assertEquals("请选择图片", error.getMessage());
    }

    @Test
    void nonImageBytesAreRejectedAsUnparseableImage() {
        UploadService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT));

        assertEquals("仅允许可解析的 JPG/PNG 等图片", error.getMessage());
    }

    @Test
    void dimensionLimitIsRejectedFromPngHeaderBeforePixelDecode() throws Exception {
        UploadService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "too-wide.png", "image/png", pngMetadataOnly(12_001, 1));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT));

        assertEquals("图片像素尺寸过大", error.getMessage());
    }

    @Test
    void totalPixelLimitIsRejectedEvenWhenEachDimensionIsUnderSideLimit() throws Exception {
        UploadService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "too-many-pixels.png", "image/png", pngMetadataOnly(7_000, 7_000));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.storeImage(file, UploadAsset.Kind.QUOTE_SCREENSHOT));

        assertEquals("图片像素尺寸过大", error.getMessage());
    }

    private UploadService service() {
        UploadService service = new UploadService(mock(UploadAssetRepository.class));
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.resolve("uploads").toString());
        return service;
    }

    private static byte[] pngMetadataOnly(int width, int height) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

            ByteArrayOutputStream ihdrBytes = new ByteArrayOutputStream();
            try (DataOutputStream ihdr = new DataOutputStream(ihdrBytes)) {
                ihdr.writeInt(width);
                ihdr.writeInt(height);
                ihdr.writeByte(8);
                ihdr.writeByte(2);
                ihdr.writeByte(0);
                ihdr.writeByte(0);
                ihdr.writeByte(0);
            }
            writeChunk(out, "IHDR", ihdrBytes.toByteArray());
            writeChunk(out, "IEND", new byte[0]);
        }
        return bytes.toByteArray();
    }

    private static void writeChunk(DataOutputStream out, String type, byte[] data) throws Exception {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
