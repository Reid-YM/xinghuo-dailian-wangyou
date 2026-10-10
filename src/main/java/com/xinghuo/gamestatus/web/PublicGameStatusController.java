package com.xinghuo.gamestatus.web;

import com.xinghuo.gamestatus.application.GameStatusApplicationService;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/public/game-status"})
public class PublicGameStatusController {
    private final GameStatusApplicationService gameStatuses;
    private final TaxonomyApplicationService taxonomy;

    @GetMapping
    Map<String, Object> gameStatus(@RequestParam(value="game", required=false) String game) {
        return GameStatusWebMapper.toPublicResponse(
                this.gameStatuses.require(this.resolveGameCode(game)));
    }

    @GetMapping(value={"/all"})
    List<Map<String, Object>> gameStatusAll() {
        return this.gameStatuses.all().stream()
                .map(GameStatusWebMapper::toSummaryResponse)
                .toList();
    }

    private String resolveGameCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "CODMW4";
        }
        String v = raw.trim();
        try {
            return this.taxonomy.publicGameBySeoSlug(v.toLowerCase(Locale.ROOT)).getCode();
        }
        catch (Exception ignore) {
            return v.toUpperCase(Locale.ROOT);
        }
    }

    @Generated
    public PublicGameStatusController(GameStatusApplicationService gameStatuses, TaxonomyApplicationService taxonomy) {
        this.gameStatuses = gameStatuses;
        this.taxonomy = taxonomy;
    }
}
