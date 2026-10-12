package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.GameStatus;
import com.xinghuo.repository.GameStatusRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameStatusService {
    private final GameStatusRepository repo;
    private final AuditService audit;

    public GameStatusService(GameStatusRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public GameStatus require(String gameCode) {
        if (gameCode == null || gameCode.isBlank()) {
            throw new BadRequestException("缺少 game 参数");
        }
        return this.repo.findById(gameCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public GameStatus resolve(String gameCode, GameStatus fallback) {
        if (gameCode == null || gameCode.isBlank()) {
            return fallback;
        }
        return this.repo.findById(gameCode.trim().toUpperCase(Locale.ROOT)).orElse(fallback);
    }

    @Transactional(readOnly = true)
    public List<GameStatus> all() {
        return this.repo.findAllByOrderByGameCodeAsc();
    }

    @Transactional
    public GameStatus update(
            String gameCode,
            String statusCode,
            String publicLabel,
            String publicDetail,
            String ctaLabel,
            String operator,
            HttpServletRequest request) {
        String code = gameCode == null ? "" : gameCode.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            throw new BadRequestException("缺少游戏代码");
        }
        if (publicLabel == null || publicLabel.isBlank()) {
            throw new BadRequestException("状态文案不能为空");
        }
        if (publicLabel.length() > 80) {
            throw new BadRequestException("状态文案过长");
        }
        if (publicDetail != null && publicDetail.length() > 240) {
            throw new BadRequestException("状态详情过长");
        }
        if (ctaLabel != null && ctaLabel.length() > 40) {
            throw new BadRequestException("按钮文案过长");
        }

        GameStatus.StatusCode parsed;
        try {
            parsed = GameStatus.StatusCode.parse(statusCode);
        }
        catch (Exception ignored) {
            throw new BadRequestException("非法运营状态");
        }

        Instant now = Instant.now();
        GameStatus status = this.repo.findById(code).orElseGet(() -> GameStatus.builder()
                .gameCode(code)
                .effectiveAt(now)
                .updatedAt(now)
                .build());
        status.setStatusCode(parsed);
        status.setPublicLabel(publicLabel.trim());
        if (publicDetail != null) {
            status.setPublicDetail(this.blankToNull(publicDetail));
        }
        if (ctaLabel != null) {
            status.setCtaLabel(this.blankToNull(ctaLabel));
        }
        status.setUpdatedBy(operator == null ? "admin" : operator);
        status.setEffectiveAt(now);
        status.setUpdatedAt(now);
        this.repo.save(status);
        this.audit.log(
                "GAME_STATUS_UPDATE",
                "GAME_STATUS",
                code,
                operator,
                null,
                status,
                "后台更新运营状态",
                request);
        return status;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
