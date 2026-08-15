package com.example.trainbooking.controller;

import com.example.trainbooking.dto.HoldSeatRequest;
import com.example.trainbooking.dto.HoldSeatResponse;
import com.example.trainbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatController {

    private final BookingService bookingService;

    @PostMapping("/hold-seat")
    public HoldSeatResponse holdSeat(@Valid @RequestBody HoldSeatRequest request) {
        return bookingService.holdSeat(request);
    }
}
