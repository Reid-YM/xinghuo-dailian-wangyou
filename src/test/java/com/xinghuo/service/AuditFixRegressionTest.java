package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.xinghuo.exception.ConflictException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.order.application.UpdateOrderCommand;
import com.xinghuo.order.infrastructure.OrderPersistenceAdapter;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.UploadAssetRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class AuditFixRegressionTest {
 @TempDir Path dir;
 @Test void transparentPngKeepsAlphaAndCorrectMime() throws Exception {
  var repo=mock(UploadAssetRepository.class);
  when(repo.save(any())).thenAnswer(i->i.getArgument(0));
  var uploads=new UploadService(repo);
  ReflectionTestUtils.setField(uploads,"uploadDir",dir.toString());
  var image=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
  image.setRGB(0,0,0x80ff0000);
  var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);
  var asset=uploads.storeImage(new MockMultipartFile("file","alpha.png","image/png",bytes.toByteArray()),UploadAsset.Kind.values()[0]);
  assertEquals("image/png",asset.getContentType());
  var decoded=ImageIO.read(dir.resolve(asset.getStoredName()).toFile());
  assertEquals(0x80ff0000,decoded.getRGB(0,0));
 }
 @Test void staleOrderCannotOverwriteNewerFields() {
  var repo=mock(CustomerOrderRepository.class);
  var order=new CustomerOrder();order.setVersion(4L);order.setProgressText("new progress");
  when(repo.findByOrderNo("XH1234567890")).thenReturn(Optional.of(order));
  var service=new OrderPersistenceAdapter(repo,mock(UploadService.class),mock(AuditService.class));
  var command=new UpdateOrderCommand(3L,"IN_PROGRESS",10,"stale progress",null,null,"test");
  assertThrows(ConflictException.class,()->service.update("XH1234567890",command,"admin"));
  assertEquals("new progress",order.getProgressText());
 }
}
