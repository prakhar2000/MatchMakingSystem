package com.matchmaking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveQueueRequest {

    @NotNull(message = "Player ID is required")
    private Long playerId;
}
