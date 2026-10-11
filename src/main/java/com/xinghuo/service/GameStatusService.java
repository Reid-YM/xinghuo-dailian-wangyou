package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.GameStatus;
import com.xinghuo.repository.GameStatusRepository;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameStatusService {
    private final GameStatusRepository repo;
    private final AuditService audit;

    @Transactional(readOnly=true)
    public GameStatus require(String gameCode) {
        if (gameCode == null || gameCode.isBlank()) {
            throw new BadRequestException("\u7f3a\u5c11 game \u53c2\u6570");
        }
        return (GameStatus)this.repo.findById(gameCode.trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new NotFoundException());
    }

    @Transactional(readOnly=true)
    public GameStatus resolve(String gameCode, GameStatus fallback) {
        if (gameCode == null || gameCode.isBlank()) {
            return fallback;
        }
        return this.repo.findById(gameCode.trim().toUpperCase(Locale.ROOT)).orElse(fallback);
    }

    @Transactional(readOnly=true)
    public List<GameStatus> all() {
        return this.repo.findAllByOrderByGameCodeAsc();
    }

    @Transactional
    public GameStatus update(String gameCode, String statusCode, String publicLabel, String publicDetail, String ctaLabel, String operator, HttpServletRequest request) {
        GameStatus.StatusCode parsed;
        String code;
        String string = code = gameCode == null ? "" : gameCode.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            throw new BadRequestException("\u7f3a\u5c11\u6e38\u620f\u4ee3\u7801");
        }
        if (publicLabel == null || publicLabel.isBlank()) {
            throw new BadRequestException("\u72b6\u6001\u6587\u6848\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (publicLabel.length() > 80) {
            throw new BadRequestException("\u72b6\u6001\u6587\u6848\u8fc7\u957f");
        }
        if (publicDetail != null && publicDetail.length() > 240) {
            throw new BadRequestException("\u72b6\u6001\u8be6\u60c5\u8fc7\u957f");
        }
        if (ctaLabel != null && ctaLabel.length() > 40) {
            throw new BadRequestException("\u6309\u94ae\u6587\u6848\u8fc7\u957f");
        }
        try {
            parsed = GameStatus.StatusCode.parse((String)statusCode);
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u8fd0\u8425\u72b6\u6001");
        }
        Instant now = Instant.now();
        GameStatus s = this.repo.findById(code).orElseGet(() -> GameStatus.builder().gameCode(code).effectiveAt(now).updatedAt(now).build());
        s.setStatusCode(parsed);
        s.setPublicLabel(publicLabel.trim());
        if (publicDetail != null) {
            s.setPublicDetail(this.blankToNull(publicDetail));
        }
        if (ctaLabel != null) {
            s.setCtaLabel(this.blankToNull(ctaLabel));
        }
        s.setUpdatedBy(operator == null ? "admin" : operator);
        s.setEffectiveAt(now);
        s.setUpdatedAt(now);
        this.repo.save(s);
        this.audit.log("GAME_STATUS_UPDATE", "GAME_STATUS", code, operator, null, s, "\u540e\u53f0\u66f4\u65b0\u8fd0\u8425\u72b6\u6001", request);
        return s;
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    @Generated
    public GameStatusService(GameStatusRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }
}

