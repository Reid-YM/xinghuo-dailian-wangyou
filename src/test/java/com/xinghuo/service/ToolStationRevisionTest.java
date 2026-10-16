package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class ToolStationRevisionTest {
    @TempDir Path directory;
    ObjectMapper mapper=new ObjectMapper();
    ToolStationService service(){var service=new ToolStationService(mapper);ReflectionTestUtils.setField(service,"uploadDir",directory.toString());return service;}
    JsonNode data(String code)throws Exception{return mapper.readTree("{\"games\":[{\"code\":\""+code+"\"}]}");}
    @Test void staleRevisionAndUnavailableAuditCannotChangeTheFile()throws Exception{
        var service=service();var original=data("original");service.save(original);String revision=service.revision(service.publicData());
        assertThrows(OptimisticLockingFailureException.class,()->service.checkedWrite(data("stale"),"old",false,()->fail("stale write reached audit")));
        assertThrows(IllegalStateException.class,()->service.checkedWrite(data("uncommitted"),revision,false,()->{throw new IllegalStateException("audit unavailable");}));assertEquals(original,service.publicData());
        service.checkedWrite(data("saved"),revision,false,()->{});assertEquals(data("saved"),service.publicData());assertEquals(original,mapper.readTree(directory.resolve("data/tool-station-games.json.bak").toFile()));
        try(var files=Files.list(directory.resolve("data"))){assertTrue(files.noneMatch(path->path.toString().endsWith(".tmp")));}
    }
    @Test void distinctServiceInstancesSerializeVersionCheckAndWrite()throws Exception{
        var first=service();var second=service();first.save(data("original"));String revision=first.revision(first.publicData());var start=new CountDownLatch(1);var audits=new AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(2)){
            Future<Boolean> one=pool.submit(()->{start.await();try{first.checkedWrite(data("first"),revision,false,audits::incrementAndGet);return true;}catch(OptimisticLockingFailureException expected){return false;}});
            Future<Boolean> two=pool.submit(()->{start.await();try{second.checkedWrite(data("second"),revision,false,audits::incrementAndGet);return true;}catch(OptimisticLockingFailureException expected){return false;}});start.countDown();assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,audits.get());assertNotEquals("original",first.publicData().path("games").get(0).path("code").asText());
    }
}
