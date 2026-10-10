package com.xinghuo.gamestatus.infrastructure;

import com.xinghuo.audit.infrastructure.AuditService;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.gamestatus.application.GameStatusCode;
import com.xinghuo.gamestatus.application.GameStatusPort;
import com.xinghuo.gamestatus.application.GameStatusView;
import com.xinghuo.gamestatus.application.UpdateGameStatusCommand;
import com.xinghuo.model.GameStatus;
import com.xinghuo.repository.GameStatusRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Game-status-owned persistence, validation, and audit adapter. */
@Component
public class GameStatusPersistenceAdapter implements GameStatusPort {
    private final GameStatusRepository repo;
    private final AuditService audit;

    public GameStatusPersistenceAdapter(GameStatusRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public GameStatusView require(String gameCode) {
        if (gameCode == null || gameCode.isBlank()) {
            throw new BadRequestException("缺少 game 参数");
        }
        return repo.findById(normalize(gameCode))
                .map(this::view)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public GameStatusView resolve(String gameCode, GameStatusView fallback) {
        if (gameCode == null || gameCode.isBlank()) {
            return fallback;
        }
        return repo.findById(normalize(gameCode)).map(this::view).orElse(fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameStatusView> all() {
        return repo.findAllByOrderByGameCodeAsc().stream().map(this::view).toList();
    }

    @Override
    @Transactional
    public GameStatusView update(UpdateGameStatusCommand command) {
        String code = command.gameCode() == null ? "" : normalize(command.gameCode());
        if (code.isBlank()) {
            throw new BadRequestException("缺少游戏代码");
        }
        if (command.publicLabel() == null || command.publicLabel().isBlank()) {
            throw new BadRequestException("状态文案不能为空");
        }
        if (command.publicLabel().length() > 80) {
            throw new BadRequestException("状态文案过长");
        }
        if (command.publicDetail() != null && command.publicDetail().length() > 240) {
            throw new BadRequestException("状态详情过长");
        }
        if (command.ctaLabel() != null && command.ctaLabel().length() > 40) {
            throw new BadRequestException("按钮文案过长");
        }

        GameStatusCode parsed;
        try {
            parsed = GameStatusCode.parse(command.statusCode());
        } catch (Exception e) {
            throw new BadRequestException("非法运营状态");
        }

        Instant now = Instant.now();
        GameStatus entity = repo.findById(code).orElseGet(() -> GameStatus.builder()
                .gameCode(code)
                .effectiveAt(now)
                .updatedAt(now)
                .build());
        entity.setStatusCode(GameStatus.StatusCode.valueOf(parsed.name()));
        entity.setPublicLabel(command.publicLabel().trim());
        if (command.publicDetail() != null) {
            entity.setPublicDetail(blankToNull(command.publicDetail()));
        }
        if (command.ctaLabel() != null) {
            entity.setCtaLabel(blankToNull(command.ctaLabel()));
        }
        entity.setUpdatedBy(command.operator() == null ? "admin" : command.operator());
        entity.setEffectiveAt(now);
        entity.setUpdatedAt(now);
        repo.save(entity);

        GameStatusView after = view(entity);
        audit.log(
                "GAME_STATUS_UPDATE",
                "GAME_STATUS",
                code,
                command.operator(),
                null,
                after,
                "后台更新运营状态",
                currentRequest());
        return after;
    }

    private String normalize(String gameCode) {
        return gameCode.trim().toUpperCase(Locale.ROOT);
    }

    private GameStatusView view(GameStatus entity) {
        return new GameStatusView(
                entity.getGameCode(),
                GameStatusCode.valueOf(entity.getStatusCode().name()),
                entity.getPublicLabel(),
                entity.getPublicDetail(),
                entity.getCtaLabel(),
                entity.getEffectiveAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
