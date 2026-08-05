package com.matchmaking.service.impl;

import com.matchmaking.dto.request.PlayerRequest;
import com.matchmaking.dto.request.PlayerUpdateRequest;
import com.matchmaking.dto.response.PlayerResponse;
import com.matchmaking.entity.Player;
import com.matchmaking.exceptions.DuplicatePlayerException;
import com.matchmaking.exceptions.ResourceNotFoundException;
import com.matchmaking.mapper.PlayerMapper;
import com.matchmaking.repository.PlayerRepository;
import com.matchmaking.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    @Override
    public PlayerResponse createPlayer(PlayerRequest playerRequest) {
        if (playerRepository.existsByUsername(playerRequest.getUsername())) {
            throw new DuplicatePlayerException("Player with username " + playerRequest.getUsername() + " already exists");
        }
        if (playerRepository.existsByEmail(playerRequest.getEmail())) {
            throw new DuplicatePlayerException("Player with email " + playerRequest.getEmail() + " already exists");
        }

        Player player = playerMapper.toEntity(playerRequest);
        Player savedPlayer = playerRepository.save(player);
        return playerMapper.toResponse(savedPlayer);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getPlayer(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getAllPlayers() {
        List<Player> players = playerRepository.findAll();
        return players.stream()
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    public PlayerResponse updatePlayer(Long id, PlayerUpdateRequest playerUpdateRequest) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));

        if (playerUpdateRequest.getUsername() != null && !playerUpdateRequest.getUsername().equals(player.getUsername())) {
            if (playerRepository.existsByUsername(playerUpdateRequest.getUsername())) {
                throw new DuplicatePlayerException("Player with username " + playerUpdateRequest.getUsername() + " already exists");
            }
            player.setUsername(playerUpdateRequest.getUsername());
        }

        if (playerUpdateRequest.getEmail() != null && !playerUpdateRequest.getEmail().equals(player.getEmail())) {
            if (playerRepository.existsByEmail(playerUpdateRequest.getEmail())) {
                throw new DuplicatePlayerException("Player with email " + playerUpdateRequest.getEmail() + " already exists");
            }
            player.setEmail(playerUpdateRequest.getEmail());
        }

        if (playerUpdateRequest.getRegion() != null) {
            player.setRegion(playerUpdateRequest.getRegion());
        }

        Player updatedPlayer = playerRepository.save(player);
        return playerMapper.toResponse(updatedPlayer);
    }

    @Override
    public void deletePlayer(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));
        playerRepository.delete(player);
    }
}
