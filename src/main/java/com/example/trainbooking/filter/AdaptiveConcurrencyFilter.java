package com.example.trainbooking.filter;

import com.example.trainbooking.metrics.AdaptiveConcurrencyController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdaptiveConcurrencyFilter extends OncePerRequestFilter {

    private final AdaptiveConcurrencyController controller;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getRequestURI().startsWith("/api/book-ticket") && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!controller.tryAcquire()) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.getWriter().write("Adaptive concurrency limit exceeded");
            return;
        }
        Instant start = Instant.now();
        boolean success = false;
        try {
            filterChain.doFilter(request, response);
            success = response.getStatus() < 500;
        } finally {
            long latency = Duration.between(start, Instant.now()).toMillis();
            controller.releaseAndRecord(latency, success);
        }
    }
}
