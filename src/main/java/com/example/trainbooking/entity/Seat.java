package com.example.trainbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "seats")
@Getter
@Setter
public class Seat {

    @Id
    private Long id;

    @Column(nullable = false)
    private String trainId;

    @Column(nullable = false)
    private String coach;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private String heldByUserId;

    private Instant holdExpiresAt;

    @Version
    private Long version;
}
