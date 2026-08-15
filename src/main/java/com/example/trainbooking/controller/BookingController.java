package com.example.trainbooking.controller;

import com.example.trainbooking.dto.BookingRequest;
import com.example.trainbooking.dto.BookingResponse;
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
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/book-ticket")
    public BookingResponse bookTicket(@Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
    }
}
