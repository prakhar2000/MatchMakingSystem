package com.matchmaking.service.impl;

import com.matchmaking.constants.PlayerStatus;
import com.matchmaking.dto.request.JoinQueueRequest;
import com.matchmaking.dto.request.LeaveQueueRequest;
import com.matchmaking.dto.response.QueueStatusResponse;
import com.matchmaking.entity.Player;
import com.matchmaking.exceptions.PlayerAlreadyInQueueException;
import com.matchmaking.exceptions.PlayerNotInQueueException;
import com.matchmaking.exceptions.ResourceNotFoundException;
import com.matchmaking.repository.PlayerRepository;
import com.matchmaking.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueServiceImpl implements QueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlayerRepository playerRepository;

    private static final String QUEUE_KEY_PREFIX = "queue:";
    private static final long QUEUE_EXPIRY_HOURS = 24;

    @Override
    public QueueStatusResponse joinQueue(JoinQueueRequest joinQueueRequest) {
        Player player = playerRepository.findById(joinQueueRequest.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + joinQueueRequest.getPlayerId()));

        if (player.getStatus() == PlayerStatus.IN_QUEUE) {
            throw new PlayerAlreadyInQueueException("Player is already in queue");
        }

        String queueKey = buildQueueKey(player.getRegion(), joinQueueRequest.getGameMode());

        // Check if player is already in this specific queue
        Boolean isMember = redisTemplate.opsForSet().isMember(queueKey, player.getId().toString());
        if (Boolean.TRUE.equals(isMember)) {
            throw new PlayerAlreadyInQueueException("Player is already in this queue");
        }

        // Add player to Redis sorted set with Elo as score
        redisTemplate.opsForZSet().add(queueKey, player.getId().toString(), player.getElo());
        redisTemplate.expire(queueKey, QUEUE_EXPIRY_HOURS, TimeUnit.HOURS);

        // Update player status
        player.setStatus(PlayerStatus.IN_QUEUE);
        playerRepository.save(player);

        // Get queue position
        Long rank = redisTemplate.opsForZSet().rank(queueKey, player.getId().toString());
        int queuePosition = rank != null ? rank.intValue() + 1 : 0;

        return QueueStatusResponse.builder()
                .playerId(player.getId())
                .status(PlayerStatus.IN_QUEUE)
                .queuePosition(queuePosition)
                .estimatedWaitTime(calculateEstimatedWaitTime(queuePosition))
                .build();
    }

    @Override
    public QueueStatusResponse leaveQueue(LeaveQueueRequest leaveQueueRequest) {
        Player player = playerRepository.findById(leaveQueueRequest.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + leaveQueueRequest.getPlayerId()));

        if (player.getStatus() != PlayerStatus.IN_QUEUE) {
            throw new PlayerNotInQueueException("Player is not in queue");
        }

        // Remove from all possible queues (player could be in any game mode queue)
        for (var gameMode : com.matchmaking.constants.GameMode.values()) {
            String queueKey = buildQueueKey(player.getRegion(), gameMode);
            redisTemplate.opsForZSet().remove(queueKey, player.getId().toString());
        }

        // Update player status
        player.setStatus(PlayerStatus.OFFLINE);
        playerRepository.save(player);

        return QueueStatusResponse.builder()
                .playerId(player.getId())
                .status(PlayerStatus.OFFLINE)
                .queuePosition(null)
                .estimatedWaitTime(null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatus(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        if (player.getStatus() != PlayerStatus.IN_QUEUE) {
            return QueueStatusResponse.builder()
                    .playerId(player.getId())
                    .status(player.getStatus())
                    .queuePosition(null)
                    .estimatedWaitTime(null)
                    .build();
        }

        // Check all possible game mode queues
        for (var gameMode : com.matchmaking.constants.GameMode.values()) {
            String queueKey = buildQueueKey(player.getRegion(), gameMode);
            Long rank = redisTemplate.opsForZSet().rank(queueKey, player.getId().toString());
            if (rank != null) {
                int queuePosition = rank.intValue() + 1;
                return QueueStatusResponse.builder()
                        .playerId(player.getId())
                        .status(PlayerStatus.IN_QUEUE)
                        .queuePosition(queuePosition)
                        .estimatedWaitTime(calculateEstimatedWaitTime(queuePosition))
                        .build();
            }
        }

        // Player status is IN_QUEUE but not found in any queue - inconsistent state
        player.setStatus(PlayerStatus.OFFLINE);
        playerRepository.save(player);

        return QueueStatusResponse.builder()
                .playerId(player.getId())
                .status(PlayerStatus.OFFLINE)
                .queuePosition(null)
                .estimatedWaitTime(null)
                .build();
    }

    private String buildQueueKey(com.matchmaking.constants.Region region, com.matchmaking.constants.GameMode gameMode) {
        return QUEUE_KEY_PREFIX + region.name() + ":" + gameMode.name();
    }

    private int calculateEstimatedWaitTime(int queuePosition) {
        // Simple estimation: 30 seconds per position ahead
        return queuePosition * 30;
    }
}
