package com.matchmaking.service.impl;

import com.matchmaking.dto.response.MatchFoundResponse;
import com.matchmaking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyMatchFound(Long playerId, MatchFoundResponse matchFoundResponse) {
        String destination = "/topic/match/" + playerId;
        log.info("Sending match found notification to player {} at destination {}", playerId, destination);
        messagingTemplate.convertAndSend(destination, matchFoundResponse);
    }

    @Override
    public void notifyQueueStatus(Long playerId, String status, Integer queuePosition) {
        String destination = "/topic/queue/" + playerId;
        log.debug("Sending queue status update to player {} at destination {}", playerId, destination);
        messagingTemplate.convertAndSend(destination, new QueueStatusUpdate(status, queuePosition));
    }

    private record QueueStatusUpdate(String status, Integer queuePosition) {}
}
