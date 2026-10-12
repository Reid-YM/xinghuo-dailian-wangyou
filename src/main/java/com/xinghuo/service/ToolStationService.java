package com.xinghuo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ToolStationService {
    private static final String BUNDLED_DATA = "static/assets/tool-station-games.json";

    private final ObjectMapper mapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ToolStationService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

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
        ClassPathResource bundled = new ClassPathResource(BUNDLED_DATA);
        try {
            Files.createDirectories(file.getParent());
            this.backupIfPresent(file);
            try (InputStream input = bundled.getInputStream()) {
                Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return this.mapper.readTree(file.toFile());
        }
        catch (Exception e) {
            throw new BadRequestException("工具站数据初始化失败");
        }
    }

    private JsonNode readOrSeed() {
        Path file = this.dataFile();
        if (!Files.exists(file)) {
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
            throw new BadRequestException("工具站数据读取失败");
        }
    }

    private void write(JsonNode data) {
        Path file = this.dataFile();
        try {
            Files.createDirectories(file.getParent());
            this.backupIfPresent(file);
            this.mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        }
        catch (Exception e) {
            throw new BadRequestException("工具站数据保存失败");
        }
    }

    private void backupIfPresent(Path file) throws Exception {
        if (Files.exists(file)) {
            Files.copy(
                    file,
                    file.resolveSibling(file.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validate(JsonNode data) {
        if (data == null || !data.isObject()) {
            throw new BadRequestException("工具站数据格式不正确");
        }
        JsonNode games = data.path("games");
        if (!games.isArray() || games.isEmpty()) {
            throw new BadRequestException("工具站至少需要保留一个游戏版本");
        }
    }

    private Path dataFile() {
        return Paths.get(this.uploadDir, "data", "tool-station-games.json").normalize();
    }
}
