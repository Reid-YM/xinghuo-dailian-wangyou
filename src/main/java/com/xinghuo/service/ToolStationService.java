package com.xinghuo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.BadRequestException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ToolStationService {
    private static final String BUNDLED_DATA = "static/assets/tool-station-games.json";
    private static final ConcurrentHashMap<Path,Object> FILE_LOCKS=new ConcurrentHashMap<>();

    private final ObjectMapper mapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ToolStationService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public synchronized JsonNode publicData() {
        return locked(this::readOrSeed);
    }

    public synchronized JsonNode save(JsonNode data) {
        return locked(()->{this.validate(data);this.write(data);return data;});
    }

    public synchronized JsonNode reset() {
        return locked(this::resetUnlocked);
    }

    /** Revision is checked under the same JVM and filesystem lock as all other writers. */
    public synchronized JsonNode checkedWrite(JsonNode data,String expected,boolean reset,Runnable beforeWrite) {
        return locked(()->{
            String actual=revision(readOrSeed());
            if(expected==null||(!actual.equals(expected)&&!("\""+actual+"\"").equals(expected)))throw new OptimisticLockingFailureException("工具站资料已变化，请先核对；当前输入尚未保存");
            if(!reset)validate(data);
            beforeWrite.run();
            if(reset)return resetUnlocked();
            write(data);return data;
        });
    }

    public String revision(JsonNode data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(data)));}catch(Exception error){throw new BadRequestException("工具站版本读取失败");}}

    private <T> T locked(Supplier<T> action){
        Path lock=dataFile().toAbsolutePath().normalize().resolveSibling("tool-station-games.lock");
        synchronized(FILE_LOCKS.computeIfAbsent(lock,key->new Object())){
            try{Files.createDirectories(lock.getParent());try(var channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE);var ignored=channel.lock()){return action.get();}}
            catch(java.io.IOException error){throw new BadRequestException("工具站资料暂时无法访问");}
        }
    }

    private JsonNode resetUnlocked() {
        Path file = this.dataFile();
        ClassPathResource bundled = new ClassPathResource(BUNDLED_DATA);
        try {
            try (InputStream input = bundled.getInputStream()) {
                JsonNode data=this.mapper.readTree(input);validate(data);write(data);return data;
            }
        }
        catch (Exception e) {
            throw new BadRequestException("工具站数据初始化失败");
        }
    }

    private JsonNode readOrSeed() {
        Path file = this.dataFile();
        if (!Files.exists(file)) {
            return this.resetUnlocked();
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
            Path temporary=Files.createTempFile(file.getParent(),"tool-station-",".json.tmp");
            try{this.mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(),data);
                try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(temporary);}
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
