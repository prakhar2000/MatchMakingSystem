package com.matchmaking.service.impl;

import com.matchmaking.constants.GameMode;
import com.matchmaking.constants.MatchStatus;
import com.matchmaking.constants.PlayerStatus;
import com.matchmaking.constants.Region;
import com.matchmaking.constants.Team;
import com.matchmaking.dto.response.MatchFoundResponse;
import com.matchmaking.entity.Match;
import com.matchmaking.entity.MatchPlayer;
import com.matchmaking.entity.Player;
import com.matchmaking.repository.MatchPlayerRepository;
import com.matchmaking.repository.MatchRepository;
import com.matchmaking.repository.PlayerRepository;
import com.matchmaking.service.MatchmakingService;
import com.matchmaking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingServiceImpl implements MatchmakingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final NotificationService notificationService;

    private static final String QUEUE_KEY_PREFIX = "queue:";
    private static final int ELO_THRESHOLD = 100;
    private static final int MIN_PLAYERS_PER_MATCH = 2;

    @Override
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void processMatchmaking() {
        log.debug("Processing matchmaking...");

        for (Region region : Region.values()) {
            for (GameMode gameMode : GameMode.values()) {
                processQueueForRegionAndMode(region, gameMode);
            }
        }
    }

    private void processQueueForRegionAndMode(Region region, GameMode gameMode) {
        String queueKey = buildQueueKey(region, gameMode);

        // Get all players in queue sorted by Elo
        Set<Object> playersInQueue = redisTemplate.opsForZSet().range(queueKey, 0, -1);

        if (playersInQueue == null || playersInQueue.size() < MIN_PLAYERS_PER_MATCH) {
            return;
        }

        // Convert to player IDs and fetch player data
        Long[] playerIds = playersInQueue.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .toArray(Long[]::new);

        // Find matches within Elo threshold
        findAndCreateMatches(queueKey, playerIds, region);
    }

    private void findAndCreateMatches(String queueKey, Long[] playerIds, Region region) {
        for (int i = 0; i < playerIds.length - 1; i++) {
            Long player1Id = playerIds[i];
            Player player1 = playerRepository.findById(player1Id).orElse(null);

            if (player1 == null || player1.getStatus() != PlayerStatus.IN_QUEUE) {
                continue;
            }

            for (int j = i + 1; j < playerIds.length; j++) {
                Long player2Id = playerIds[j];
                Player player2 = playerRepository.findById(player2Id).orElse(null);

                if (player2 == null || player2.getStatus() != PlayerStatus.IN_QUEUE) {
                    continue;
                }

                // Check Elo difference
                int eloDifference = Math.abs(player1.getElo() - player2.getElo());

                if (eloDifference <= ELO_THRESHOLD) {
                    // Create match
                    createMatch(player1, player2, region, queueKey);
                    break; // Player1 is now matched, move to next
                }
            }
        }
    }

    private void createMatch(Player player1, Player player2, Region region, String queueKey) {
        log.info("Creating match between player {} (Elo: {}) and player {} (Elo: {})",
                player1.getId(), player1.getElo(), player2.getId(), player2.getElo());

        // Calculate average Elo
        int averageElo = (player1.getElo() + player2.getElo()) / 2;

        // Create match
        Match match = Match.builder()
                .averageElo(averageElo)
                .region(region)
                .status(MatchStatus.PENDING)
                .build();

        match = matchRepository.save(match);

        // Create match players
        MatchPlayer matchPlayer1 = MatchPlayer.builder()
                .match(match)
                .playerId(player1.getId())
                .playerElo(player1.getElo())
                .team(Team.TEAM_A)
                .build();

        MatchPlayer matchPlayer2 = MatchPlayer.builder()
                .match(match)
                .playerId(player2.getId())
                .playerElo(player2.getElo())
                .team(Team.TEAM_B)
                .build();

        matchPlayerRepository.save(matchPlayer1);
        matchPlayerRepository.save(matchPlayer2);

        // Remove players from queue
        redisTemplate.opsForZSet().remove(queueKey, player1.getId().toString());
        redisTemplate.opsForZSet().remove(queueKey, player2.getId().toString());

        // Update player statuses
        player1.setStatus(PlayerStatus.IN_MATCH);
        player2.setStatus(PlayerStatus.IN_MATCH);
        playerRepository.save(player1);
        playerRepository.save(player2);

        // Send WebSocket notifications
        MatchFoundResponse response1 = MatchFoundResponse.builder()
                .matchId(match.getId())
                .playerId(player1.getId())
                .playerElo(player1.getElo())
                .team(Team.TEAM_A)
                .region(region)
                .matchCreatedAt(match.getCreatedAt())
                .message("Match found! You are on Team A")
                .build();

        MatchFoundResponse response2 = MatchFoundResponse.builder()
                .matchId(match.getId())
                .playerId(player2.getId())
                .playerElo(player2.getElo())
                .team(Team.TEAM_B)
                .region(region)
                .matchCreatedAt(match.getCreatedAt())
                .message("Match found! You are on Team B")
                .build();

        notificationService.notifyMatchFound(player1.getId(), response1);
        notificationService.notifyMatchFound(player2.getId(), response2);

        log.info("Match {} created successfully", match.getId());
    }

    private String buildQueueKey(Region region, GameMode gameMode) {
        return QUEUE_KEY_PREFIX + region.name() + ":" + gameMode.name();
    }
}
