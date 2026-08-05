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
import com.matchmaking.ml.feature.MatchFeatureExtractor;
import com.matchmaking.ml.feature.MatchFeatures;
import com.matchmaking.ml.model.MatchCandidate;
import com.matchmaking.ml.scorer.MatchQualityScorer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingServiceImpl implements MatchmakingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final NotificationService notificationService;
    private final MatchFeatureExtractor featureExtractor;
    private final MatchQualityScorer matchQualityScorer;

    private static final String QUEUE_KEY_PREFIX = "queue:";
    private static final int ELO_THRESHOLD = 100;
    private static final int MIN_PLAYERS_PER_MATCH = 2;
    private static final int MAX_CANDIDATES = 10;

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
        // Generate match candidates using sliding window
        List<MatchCandidate> candidates = generateMatchCandidates(playerIds, region);

        if (candidates.isEmpty()) {
            return;
        }

        // Score each candidate and select the best one
        MatchCandidate bestCandidate = candidates.stream()
                .max((c1, c2) -> Double.compare(c1.getQualityScore(), c2.getQualityScore()))
                .orElse(null);

        if (bestCandidate != null && bestCandidate.getQualityScore() > 0.5) {
            // Create match from best candidate
            List<Player> players = bestCandidate.getPlayers();
            if (players.size() == 2) {
                createMatch(players.get(0), players.get(1), region, queueKey);
            } else if (players.size() > 2) {
                createMultiPlayerMatch(players, region, queueKey);
            }
        }
    }

    private List<MatchCandidate> generateMatchCandidates(Long[] playerIds, Region region) {
        List<MatchCandidate> candidates = new ArrayList<>();

        // Generate candidates using sliding window approach
        for (int i = 0; i < Math.min(playerIds.length - 1, MAX_CANDIDATES); i++) {
            for (int j = i + 1; j < Math.min(i + MIN_PLAYERS_PER_MATCH + 2, playerIds.length); j++) {
                Long player1Id = playerIds[i];
                Long player2Id = playerIds[j];

                Player player1 = playerRepository.findById(player1Id).orElse(null);
                Player player2 = playerRepository.findById(player2Id).orElse(null);

                if (player1 == null || player2 == null ||
                    player1.getStatus() != PlayerStatus.IN_QUEUE ||
                    player2.getStatus() != PlayerStatus.IN_QUEUE) {
                    continue;
                }

                // Check Elo difference
                int eloDifference = Math.abs(player1.getElo() - player2.getElo());
                if (eloDifference > ELO_THRESHOLD) {
                    continue;
                }

                // Create candidate
                List<Player> players = List.of(player1, player2);
                MatchCandidate candidate = createAndScoreCandidate(players, region);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }

        log.debug("Generated {} match candidates for region {}", candidates.size(), region);
        return candidates;
    }

    private MatchCandidate createAndScoreCandidate(List<Player> players, Region region) {
        try {
            // Extract features
            MatchFeatures features = featureExtractor.extractFeatures(players);

            // Score using the configured scorer
            double qualityScore = matchQualityScorer.score(features);

            // Calculate candidate statistics
            double averageElo = players.stream().mapToInt(Player::getElo).average().orElse(0);
            double maxEloDifference = players.stream().mapToInt(Player::getElo).max().getAsInt() -
                                       players.stream().mapToInt(Player::getElo).min().getAsInt();

            return MatchCandidate.builder()
                    .players(players)
                    .qualityScore(qualityScore)
                    .averageElo(averageElo)
                    .maxEloDifference(maxEloDifference)
                    .eloStdDeviation(features.getEloStdDeviation())
                    .build();
        } catch (Exception e) {
            log.error("Error creating match candidate", e);
            return null;
        }
    }

    private void createMultiPlayerMatch(List<Player> players, Region region, String queueKey) {
        log.info("Creating multi-player match with {} players", players.size());

        // Calculate average Elo
        double averageElo = players.stream().mapToInt(Player::getElo).average().orElse(0);

        // Create match
        Match match = Match.builder()
                .averageElo((int) averageElo)
                .region(region)
                .status(MatchStatus.PENDING)
                .build();

        match = matchRepository.save(match);

        // Create match players with team assignment
        int half = players.size() / 2;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Team team = i < half ? Team.TEAM_A : Team.TEAM_B;

            MatchPlayer matchPlayer = MatchPlayer.builder()
                    .match(match)
                    .playerId(player.getId())
                    .playerElo(player.getElo())
                    .team(team)
                    .build();

            matchPlayerRepository.save(matchPlayer);

            // Send notification
            MatchFoundResponse response = MatchFoundResponse.builder()
                    .matchId(match.getId())
                    .playerId(player.getId())
                    .playerElo(player.getElo())
                    .team(team)
                    .region(region)
                    .matchCreatedAt(match.getCreatedAt())
                    .message("Match found! You are on " + team.name())
                    .build();

            notificationService.notifyMatchFound(player.getId(), response);

            // Remove from queue and update status
            redisTemplate.opsForZSet().remove(queueKey, player.getId().toString());
            player.setStatus(PlayerStatus.IN_MATCH);
            playerRepository.save(player);
        }

        log.info("Multi-player match {} created successfully", match.getId());
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
