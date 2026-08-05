package com.matchmaking.service;

import com.matchmaking.dto.request.JoinQueueRequest;
import com.matchmaking.dto.request.LeaveQueueRequest;
import com.matchmaking.dto.response.QueueStatusResponse;

public interface QueueService {

    QueueStatusResponse joinQueue(JoinQueueRequest joinQueueRequest);

    QueueStatusResponse leaveQueue(LeaveQueueRequest leaveQueueRequest);

    QueueStatusResponse getQueueStatus(Long playerId);
}
