package com.matchmaking.controller;

import com.matchmaking.dto.request.JoinQueueRequest;
import com.matchmaking.dto.request.LeaveQueueRequest;
import com.matchmaking.dto.response.QueueStatusResponse;
import com.matchmaking.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/join")
    public ResponseEntity<QueueStatusResponse> joinQueue(@Valid @RequestBody JoinQueueRequest joinQueueRequest) {
        QueueStatusResponse response = queueService.joinQueue(joinQueueRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<QueueStatusResponse> leaveQueue(@Valid @RequestBody LeaveQueueRequest leaveQueueRequest) {
        QueueStatusResponse response = queueService.leaveQueue(leaveQueueRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{playerId}")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@PathVariable Long playerId) {
        QueueStatusResponse response = queueService.getQueueStatus(playerId);
        return ResponseEntity.ok(response);
    }
}
