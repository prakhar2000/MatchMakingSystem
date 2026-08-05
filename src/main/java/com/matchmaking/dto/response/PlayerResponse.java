package com.matchmaking.dto.response;

import com.matchmaking.constants.PlayerStatus;
import com.matchmaking.constants.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponse {

    private Long id;
    private String username;
    private String email;
    private Integer elo;
    private Integer wins;
    private Integer losses;
    private Region region;
    private PlayerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
