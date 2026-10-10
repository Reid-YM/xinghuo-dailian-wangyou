package com.xinghuo.gamestatus.application;

import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for game operating-status reads and updates. */
@Service
public class GameStatusApplicationService {
    private final GameStatusPort port;

    public GameStatusApplicationService(GameStatusPort port) {
        this.port = port;
    }

    public GameStatusView require(String gameCode) {
        return port.require(gameCode);
    }

    public GameStatusView resolve(String gameCode, GameStatusView fallback) {
        return port.resolve(gameCode, fallback);
    }

    public List<GameStatusView> all() {
        return port.all();
    }

    public GameStatusView update(UpdateGameStatusCommand command) {
        return port.update(command);
    }
}
