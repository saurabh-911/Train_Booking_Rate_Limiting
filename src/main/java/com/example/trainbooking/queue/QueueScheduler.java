package com.example.trainbooking.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueService queueService;

    @Scheduled(fixedDelayString = "${traffic-control.queue.scheduler-ms:1000}")
    public void release() {
        queueService.releaseUsers();
    }
}
