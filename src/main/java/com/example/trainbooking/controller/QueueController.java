package com.example.trainbooking.controller;

import com.example.trainbooking.dto.QueueStatusResponse;
import com.example.trainbooking.queue.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @GetMapping("/queue-status")
    public QueueStatusResponse queueStatus(@RequestParam String userId) {
        return queueService.getStatus(userId);
    }
}
