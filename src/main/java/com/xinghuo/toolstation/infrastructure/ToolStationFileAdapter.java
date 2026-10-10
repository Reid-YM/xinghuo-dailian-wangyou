package com.xinghuo.toolstation.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.toolstation.application.ToolStationPort;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** File-system adapter for the tool-station JSON document and its backup. */
@Component
public class ToolStationFileAdapter implements ToolStationPort {
    private final ObjectMapper mapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ToolStationFileAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public synchronized String publicData() {
        return readOrSeed();
    }

    @Override
    public synchronized String save(String json) {
        JsonNode data = parseForSave(json);
        validate(data);
        write(data);
        return data.toString();
    }

    @Override
    public synchronized String reset() {
        Path file = dataFile();
        ClassPathResource bundled = new ClassPathResource("static/assets/tool-station-games.json");
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream input = bundled.getInputStream()) {
                Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return mapper.readTree(file.toFile()).toString();
        } catch (Exception e) {
            throw new BadRequestException("工具站数据初始化失败");
        }
    }

    private String readOrSeed() {
        Path file = dataFile();
        if (!Files.exists(file)) {
            return reset();
        }
        try {
            JsonNode data = mapper.readTree(file.toFile());
            validate(data);
            return data.toString();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("工具站数据读取失败");
        }
    }

    private JsonNode parseForSave(String json) {
        try {
            if (json == null) {
                return null;
            }
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new BadRequestException("工具站数据格式不正确");
        }
    }

    private void write(JsonNode data) {
        Path file = dataFile();
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        } catch (Exception e) {
            throw new BadRequestException("工具站数据保存失败");
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
        return Paths.get(uploadDir, "data", "tool-station-games.json").normalize();
    }
}
