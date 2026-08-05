package com.matchmaking.dto.response;

import com.matchmaking.constants.Region;
import com.matchmaking.constants.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFoundResponse {

    private Long matchId;
    private Long playerId;
    private Integer playerElo;
    private Team team;
    private Region region;
    private LocalDateTime matchCreatedAt;
    private String message;
}
