package com.matchmaking.mapper;

import com.matchmaking.dto.request.PlayerRequest;
import com.matchmaking.dto.response.PlayerResponse;
import com.matchmaking.entity.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public Player toEntity(PlayerRequest playerRequest) {
        return Player.builder()
                .username(playerRequest.getUsername())
                .email(playerRequest.getEmail())
                .region(playerRequest.getRegion())
                .elo(1000)
                .wins(0)
                .losses(0)
                .build();
    }

    public PlayerResponse toResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .username(player.getUsername())
                .email(player.getEmail())
                .elo(player.getElo())
                .wins(player.getWins())
                .losses(player.getLosses())
                .region(player.getRegion())
                .status(player.getStatus())
                .createdAt(player.getCreatedAt())
                .updatedAt(player.getUpdatedAt())
                .build();
    }
}
