package com.matchmaking.dto.request;

import com.matchmaking.constants.GameMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinQueueRequest {

    @NotNull(message = "Player ID is required")
    private Long playerId;

    @NotNull(message = "Game mode is required")
    private GameMode gameMode;
}
