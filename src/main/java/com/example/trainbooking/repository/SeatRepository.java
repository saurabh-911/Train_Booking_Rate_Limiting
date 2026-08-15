package com.example.trainbooking.repository;

import com.example.trainbooking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :seatId")
    Optional<Seat> findSeatForUpdate(@Param("seatId") Long seatId);

    @Modifying
    @Query("""
            update Seat s
               set s.status = com.example.trainbooking.entity.SeatStatus.AVAILABLE,
                   s.heldByUserId = null,
                   s.holdExpiresAt = null
             where s.status = com.example.trainbooking.entity.SeatStatus.HELD
               and s.holdExpiresAt < :now
            """)
    int releaseExpiredHolds(@Param("now") Instant now);
}
