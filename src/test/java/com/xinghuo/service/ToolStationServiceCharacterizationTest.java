package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class ToolStationServiceCharacterizationTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void publicDataSeedsBundledJsonWhenPersistentFileIsMissing() throws Exception {
        ToolStationService service = service();
        Path file = dataFile();
        assertFalse(Files.exists(file));

        JsonNode data = service.publicData();

        assertTrue(data.isObject());
        assertTrue(data.path("games").isArray());
        assertFalse(data.path("games").isEmpty());
        assertTrue(Files.isRegularFile(file));
        assertEquals(data, mapper.readTree(file.toFile()));
    }

    @Test
    void saveBacksUpExistingFileBeforeWritingReplacement() throws Exception {
        ToolStationService service = service();
        Path file = dataFile();
        Files.createDirectories(file.getParent());
        JsonNode oldData = mapper.readTree("{\"games\":[{\"code\":\"old\"}]}");
        JsonNode newData = mapper.readTree("{\"games\":[{\"code\":\"new\"}]}");
        mapper.writeValue(file.toFile(), oldData);

        JsonNode returned = service.save(newData);

        assertSame(newData, returned);
        assertEquals(newData, mapper.readTree(file.toFile()));
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        assertTrue(Files.isRegularFile(backup));
        assertEquals(oldData, mapper.readTree(backup.toFile()));
    }

    @Test
    void saveRejectsMissingGamesBeforeCreatingPersistentFile() throws Exception {
        ToolStationService service = service();
        JsonNode invalid = mapper.readTree("{}");

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.save(invalid));

        assertEquals("工具站至少需要保留一个游戏版本", error.getMessage());
        assertFalse(Files.exists(dataFile()));
    }

    @Test
    void publicDataRethrowsPersistedValidationErrorWithoutReplacingFile() throws Exception {
        ToolStationService service = service();
        Path file = dataFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"games\":[]}");

        BadRequestException error = assertThrows(BadRequestException.class, service::publicData);

        assertEquals("工具站至少需要保留一个游戏版本", error.getMessage());
        assertEquals(mapper.readTree("{\"games\":[]}"), mapper.readTree(file.toFile()));
    }

    @Test
    void publicDataWrapsMalformedJsonAsReadFailure() throws Exception {
        ToolStationService service = service();
        Path file = dataFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{not-json");

        BadRequestException error = assertThrows(BadRequestException.class, service::publicData);

        assertEquals("工具站数据读取失败", error.getMessage());
    }

    private ToolStationService service() {
        ToolStationService service = new ToolStationService(mapper);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        return service;
    }

    private Path dataFile() {
        return tempDir.resolve("data/tool-station-games.json");
    }
}
