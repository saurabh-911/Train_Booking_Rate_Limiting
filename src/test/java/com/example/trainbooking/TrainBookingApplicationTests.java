package com.example.trainbooking;

import com.example.trainbooking.kafka.BookingConfirmedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class TrainBookingApplicationTests {

    @MockBean
    StringRedisTemplate redisTemplate;

    @MockBean
    KafkaTemplate<String, BookingConfirmedEvent> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
