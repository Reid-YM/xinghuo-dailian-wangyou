package com.xinghuo.gamestatus.web;

import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import java.util.Map;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin/game-status"})
public class AdminGameStatusController {
    private final GameStatusApplicationService gameStatuses;

    @PutMapping(value={"/{gameCode}"})
    Map<String, Object> updateGameStatus(@PathVariable String gameCode, @RequestBody Map<String, Object> body, Authentication auth) {
        return GameStatusWebMapper.toAdminResponse(
                this.gameStatuses.update(GameStatusWebMapper.toCommand(
                        gameCode,
                        body,
                        auth == null ? "admin" : auth.getName())));
    }

    @Generated
    public AdminGameStatusController(GameStatusApplicationService gameStatuses) {
        this.gameStatuses = gameStatuses;
    }
}
