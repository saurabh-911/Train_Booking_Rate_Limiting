package com.example.trainbooking.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(topics = "booking-confirmed", groupId = "notification-service")
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Notification sent for bookingId={} user={}", event.bookingId(), event.userId());
    }
}
