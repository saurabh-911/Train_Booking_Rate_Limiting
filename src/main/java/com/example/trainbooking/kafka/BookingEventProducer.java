package com.example.trainbooking.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, BookingConfirmedEvent> kafkaTemplate;

    public void publish(BookingConfirmedEvent event) {
        kafkaTemplate.send("booking-confirmed", event.userId(), event);
        log.info("Published booking event for bookingId={}", event.bookingId());
    }
}
