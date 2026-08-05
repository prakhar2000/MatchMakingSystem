package com.matchmaking.controller;

import com.matchmaking.dto.request.PlayerRequest;
import com.matchmaking.dto.request.PlayerUpdateRequest;
import com.matchmaking.dto.response.PlayerResponse;
import com.matchmaking.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody PlayerRequest playerRequest) {
        PlayerResponse playerResponse = playerService.createPlayer(playerRequest);
        return new ResponseEntity<>(playerResponse, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getAllPlayers() {
        List<PlayerResponse> players = playerService.getAllPlayers();
        return ResponseEntity.ok(players);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id) {
        PlayerResponse playerResponse = playerService.getPlayer(id);
        return ResponseEntity.ok(playerResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponse> updatePlayer(
            @PathVariable Long id,
            @Valid @RequestBody PlayerUpdateRequest playerUpdateRequest) {
        PlayerResponse playerResponse = playerService.updatePlayer(id, playerUpdateRequest);
        return ResponseEntity.ok(playerResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
