package com.matchmaking.service;

import com.matchmaking.dto.response.MatchFoundResponse;

public interface NotificationService {

    void notifyMatchFound(Long playerId, MatchFoundResponse matchFoundResponse);

    void notifyQueueStatus(Long playerId, String status, Integer queuePosition);
}
