package com.matchmaking.service;

import com.matchmaking.dto.request.PlayerRequest;
import com.matchmaking.dto.request.PlayerUpdateRequest;
import com.matchmaking.dto.response.PlayerResponse;

import java.util.List;

public interface PlayerService {

    PlayerResponse createPlayer(PlayerRequest playerRequest);

    PlayerResponse getPlayer(Long id);

    List<PlayerResponse> getAllPlayers();

    PlayerResponse updatePlayer(Long id, PlayerUpdateRequest playerUpdateRequest);

    void deletePlayer(Long id);
}
