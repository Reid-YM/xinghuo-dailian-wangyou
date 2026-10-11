package com.xinghuo.order.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.order.application.OrderApplicationService;
import com.xinghuo.upload.infrastructure.UploadAsset;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin/orders"})
public class AdminOrderController {
    private final OrderApplicationService orders;
    private final UploadService uploads;

    @PatchMapping(value={"/{orderNo}/public-case"})
    Map<String, Object> setPublicCase(@PathVariable String orderNo, @RequestBody Map<String, Object> body, Authentication auth) {
        boolean enabled = Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", "false")));
        String summary = str(body.get("summary"));
        return OrderWebMapper.toResponse(this.orders.setPublicCase(
                orderNo, enabled, summary, auth == null ? "admin" : auth.getName()));
    }

    @GetMapping
    List<ApiDtos.AdminOrderResponse> orders() {
        return OrderWebMapper.toAdminResponses(this.orders.list());
    }

    @PostMapping
    ApiDtos.AdminOrderResponse createOrder(@Valid @RequestBody ApiDtos.CreateOrderRequest r, Authentication a, HttpServletRequest req) {
        return OrderWebMapper.toResponse(
                this.orders.create(OrderWebMapper.toCommand(r), a.getName()));
    }

    @PatchMapping(value={"/{orderNo}"})
    ApiDtos.AdminOrderResponse updateOrder(@PathVariable String orderNo, @Valid @RequestBody ApiDtos.UpdateOrderRequest r, Authentication a, HttpServletRequest req) {
        return OrderWebMapper.toResponse(
                this.orders.update(orderNo, OrderWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/{orderNo}/image"})
    ApiDtos.AdminOrderResponse orderImage(@PathVariable String orderNo, @RequestParam(value="file") MultipartFile file, Authentication a, HttpServletRequest req) {
        UploadAsset x = this.uploads.storeImage(file, UploadAsset.Kind.ORDER_PROGRESS);
        return OrderWebMapper.toResponse(this.orders.attachImage(orderNo, x.getId(), a.getName()));
    }

    @DeleteMapping(value={"/{orderNo}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    ResponseEntity<Void> deleteOrder(@PathVariable String orderNo, Authentication a, HttpServletRequest req) {
        this.orders.delete(orderNo, a.getName());
        return ResponseEntity.noContent().build();
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    @Generated
    public AdminOrderController(OrderApplicationService orders, UploadService uploads) {
        this.orders = orders;
        this.uploads = uploads;
    }
}
