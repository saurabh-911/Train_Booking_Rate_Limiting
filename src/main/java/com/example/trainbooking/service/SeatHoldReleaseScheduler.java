package com.example.trainbooking.service;

import com.example.trainbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldReleaseScheduler {

    private final SeatRepository seatRepository;

    @Transactional
    @Scheduled(fixedDelayString = "${booking.hold-release-ms:10000}")
    public void releaseExpiredHolds() {
        int released = seatRepository.releaseExpiredHolds(Instant.now());
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
    }
}
