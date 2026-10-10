package com.xinghuo.gamestatus.application;

import java.util.List;

/** Application-facing game-status persistence contract. */
public interface GameStatusPort {
    GameStatusView require(String gameCode);

    GameStatusView resolve(String gameCode, GameStatusView fallback);

    List<GameStatusView> all();

    GameStatusView update(UpdateGameStatusCommand command);
}
