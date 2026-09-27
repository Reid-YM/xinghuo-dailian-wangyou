/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.service.ToolStationService
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.io.ClassPathResource
 *  org.springframework.stereotype.Service
 */
package com.xinghuo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ToolStationService {
    private final ObjectMapper mapper;
    @Value(value="${app.upload-dir}")
    private String uploadDir;

    public synchronized JsonNode publicData() {
        return this.readOrSeed();
    }

    public synchronized JsonNode save(JsonNode data) {
        this.validate(data);
        this.write(data);
        return data;
    }

    public synchronized JsonNode reset() {
        Path file = this.dataFile();
        ClassPathResource bundled = new ClassPathResource("static/assets/tool-station-games.json");
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            if (Files.exists(file, new LinkOption[0])) {
                Files.copy(file, file.resolveSibling(String.valueOf(file.getFileName()) + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream input = bundled.getInputStream();){
                Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return this.mapper.readTree(file.toFile());
        }
        catch (Exception e) {
            throw new BadRequestException("\u5de5\u5177\u7ad9\u6570\u636e\u521d\u59cb\u5316\u5931\u8d25");
        }
    }

    private JsonNode readOrSeed() {
        Path file = this.dataFile();
        if (!Files.exists(file, new LinkOption[0])) {
            return this.reset();
        }
        try {
            JsonNode data = this.mapper.readTree(file.toFile());
            this.validate(data);
            return data;
        }
        catch (BadRequestException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BadRequestException("\u5de5\u5177\u7ad9\u6570\u636e\u8bfb\u53d6\u5931\u8d25");
        }
    }

    private void write(JsonNode data) {
        Path file = this.dataFile();
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            if (Files.exists(file, new LinkOption[0])) {
                Files.copy(file, file.resolveSibling(String.valueOf(file.getFileName()) + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            this.mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), (Object)data);
        }
        catch (Exception e) {
            throw new BadRequestException("\u5de5\u5177\u7ad9\u6570\u636e\u4fdd\u5b58\u5931\u8d25");
        }
    }

    private void validate(JsonNode data) {
        if (data == null || !data.isObject()) {
            throw new BadRequestException("\u5de5\u5177\u7ad9\u6570\u636e\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        JsonNode games = data.path("games");
        if (!games.isArray() || games.isEmpty()) {
            throw new BadRequestException("\u5de5\u5177\u7ad9\u81f3\u5c11\u9700\u8981\u4fdd\u7559\u4e00\u4e2a\u6e38\u620f\u7248\u672c");
        }
    }

    private Path dataFile() {
        return Paths.get(this.uploadDir, "data", "tool-station-games.json").normalize();
    }

    @Generated
    public ToolStationService(ObjectMapper mapper) {
        this.mapper = mapper;
    }
}

