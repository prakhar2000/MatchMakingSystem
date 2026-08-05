package com.matchmaking.dto.response;

import com.matchmaking.constants.PlayerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusResponse {

    private Long playerId;
    private PlayerStatus status;
    private Integer queuePosition;
    private Integer estimatedWaitTime;
}
