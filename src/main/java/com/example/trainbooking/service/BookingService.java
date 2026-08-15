package com.example.trainbooking.service;

import com.example.trainbooking.dto.BookingRequest;
import com.example.trainbooking.dto.BookingResponse;
import com.example.trainbooking.dto.HoldSeatRequest;
import com.example.trainbooking.dto.HoldSeatResponse;
import com.example.trainbooking.entity.Booking;
import com.example.trainbooking.entity.IdempotencyRecord;
import com.example.trainbooking.entity.Seat;
import com.example.trainbooking.entity.SeatStatus;
import com.example.trainbooking.exception.BookingFailedException;
import com.example.trainbooking.exception.QueueNotReadyException;
import com.example.trainbooking.exception.ServiceUnavailableException;
import com.example.trainbooking.kafka.BookingConfirmedEvent;
import com.example.trainbooking.kafka.BookingEventProducer;
import com.example.trainbooking.queue.QueueService;
import com.example.trainbooking.repository.BookingRepository;
import com.example.trainbooking.repository.IdempotencyRecordRepository;
import com.example.trainbooking.repository.SeatRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class BookingService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final BookingEventProducer bookingEventProducer;
    private final QueueService queueService;
    private final Counter bookingSuccessCounter;
    private final Counter bookingFailedCounter;

    public BookingService(SeatRepository seatRepository,
                          BookingRepository bookingRepository,
                          IdempotencyRecordRepository idempotencyRecordRepository,
                          BookingEventProducer bookingEventProducer,
                          QueueService queueService,
                          MeterRegistry meterRegistry) {
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.bookingEventProducer = bookingEventProducer;
        this.queueService = queueService;
        this.bookingSuccessCounter = meterRegistry.counter("booking.success.count");
        this.bookingFailedCounter = meterRegistry.counter("booking.failed.count");
    }

    @Transactional
    public HoldSeatResponse holdSeat(HoldSeatRequest request) {
        Seat seat = seatRepository.findSeatForUpdate(request.seatId())
                .orElseThrow(() -> new BookingFailedException("Seat not found"));

        if (!seat.getTrainId().equals(request.trainId())) {
            throw new BookingFailedException("Seat does not belong to requested train");
        }

        Instant now = Instant.now();
        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new BookingFailedException("Seat already booked");
        }

        if (seat.getStatus() == SeatStatus.HELD && seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isAfter(now)
                && !request.userId().equals(seat.getHeldByUserId())) {
            throw new BookingFailedException("Seat is temporarily held by another user");
        }

        seat.setStatus(SeatStatus.HELD);
        seat.setHeldByUserId(request.userId());
        seat.setHoldExpiresAt(now.plusSeconds(300));
        seatRepository.save(seat);

        return new HoldSeatResponse(seat.getId(), seat.getStatus().name(), seat.getHoldExpiresAt(), "Seat held for 5 minutes");
    }

    @Transactional
    @CircuitBreaker(name = "bookingDb", fallbackMethod = "bookingFallback")
    public BookingResponse createBooking(BookingRequest request) {
        queueService.enqueueIfAbsent(request.userId(), request.userTier());

        if (!queueService.isAdmitted(request.userId())) {
            throw new QueueNotReadyException("User is in waiting room. Poll /queue-status.");
        }

        if (!queueService.validateQueueToken(request.userId(), request.queueToken())) {
            throw new QueueNotReadyException("Queue token missing/expired. Fetch /queue-status for a fresh token.");
        }

        var existing = idempotencyRecordRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return new BookingResponse(existing.get().getBookingId(), "CONFIRMED", "Idempotent replay served");
        }

        Seat seat = seatRepository.findSeatForUpdate(request.seatId())
                .orElseThrow(() -> new BookingFailedException("Seat not found"));

        Instant now = Instant.now();
        if (seat.getStatus() == SeatStatus.BOOKED) {
            bookingFailedCounter.increment();
            throw new BookingFailedException("Seat already booked");
        }

        if (seat.getStatus() != SeatStatus.HELD
                || seat.getHoldExpiresAt() == null
                || seat.getHoldExpiresAt().isBefore(now)
                || !request.userId().equals(seat.getHeldByUserId())) {
            bookingFailedCounter.increment();
            throw new BookingFailedException("Seat hold missing/expired. Call /api/hold-seat again.");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seat.setHeldByUserId(null);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        Booking booking = new Booking();
        booking.setUserId(request.userId());
        booking.setSeatId(request.seatId());
        booking.setTrainId(request.trainId());
        booking.setCreatedAt(now);
        bookingRepository.save(booking);

        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(request.idempotencyKey());
        record.setUserId(request.userId());
        record.setBookingId(booking.getId());
        idempotencyRecordRepository.save(record);

        queueService.consumeAdmission(request.userId());
        bookingEventProducer.publish(new BookingConfirmedEvent(
                booking.getId(), request.userId(), request.seatId(), request.trainId(), booking.getCreatedAt()
        ));

        bookingSuccessCounter.increment();
        log.info("Booking confirmed bookingId={} user={} seat={}", booking.getId(), request.userId(), request.seatId());
        return new BookingResponse(booking.getId(), "CONFIRMED", "Seat booked successfully");
    }

    public BookingResponse bookingFallback(BookingRequest request, Throwable throwable) {
        throw new ServiceUnavailableException("Booking service temporarily unavailable due to downstream instability");
    }
}
