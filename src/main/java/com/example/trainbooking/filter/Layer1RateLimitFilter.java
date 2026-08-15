package com.example.trainbooking.filter;

import com.example.trainbooking.exception.RateLimitException;
import com.example.trainbooking.service.TokenBucketRateLimiterService;
import com.example.trainbooking.service.UserKeyResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class Layer1RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucketRateLimiterService rateLimiterService;
    private final UserKeyResolver userKeyResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/book-ticket");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String principalKey = userKeyResolver.resolve(request);
        if (!rateLimiterService.tryConsume(principalKey)) {
            throw new RateLimitException("Layer-1 token bucket exceeded. Retry later.");
        }
        filterChain.doFilter(request, response);
    }
}
