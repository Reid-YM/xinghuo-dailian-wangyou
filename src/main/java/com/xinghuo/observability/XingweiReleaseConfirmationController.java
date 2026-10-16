package com.xinghuo.observability;

import com.xinghuo.security.XingweiPermissionGuard;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/xingwei/release-confirmation")
@PreAuthorize("@xingweiPermissionGuard.has(authentication,'deploy:view')")
public class XingweiReleaseConfirmationController {
    private final XingweiReleaseConfirmationService service;
    private final XingweiPermissionGuard guard;

    public XingweiReleaseConfirmationController(
            XingweiReleaseConfirmationService service,
            XingweiPermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @GetMapping
    public ResponseEntity<View> snapshot(Authentication authentication) {
        boolean canConfirm = guard.resolve(authentication).allowAll();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new View(service.snapshot(), canConfirm));
    }

    @PostMapping
    @PreAuthorize("@xingweiPermissionGuard.resolve(authentication).allowAll()")
    public ResponseEntity<?> confirm(
            @RequestBody XingweiReleaseConfirmationService.Command command,
            Authentication authentication) {
        try {
            XingweiReleaseConfirmationService.Snapshot snapshot =
                    service.confirm(command, authentication.getName());
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(new View(snapshot, true));
        } catch (XingweiReleaseConfirmationService.Conflict ex) {
            return error(HttpStatus.CONFLICT, ex.getMessage());
        } catch (XingweiReleaseConfirmationService.Rejected ex) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        } catch (XingweiReleaseConfirmationService.StorageUnavailable ex) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "人工确认存储暂不可用，请稍后复核");
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("error", message));
    }

    public record View(XingweiReleaseConfirmationService.Snapshot confirmation, boolean canConfirm) {}
}
