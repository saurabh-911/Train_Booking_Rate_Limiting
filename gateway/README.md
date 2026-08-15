# Dedicated API Gateway (Layer-1)

This repository keeps runtime rate limiting in the booking service for simplicity, but this folder contains
production-ready **Spring Cloud Gateway + Redis** configuration patterns to deploy as a separate edge service.

## User KeyResolver example

```java
@Bean
KeyResolver userKeyResolver() {
  return exchange -> Mono.just(
      Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
          .orElse(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));
}
```

Deploy this gateway in front of booking-service to enforce token-bucket throttling per user/API key.
