# Train Booking System Deep Dive

This document explains the complete thought process, architecture decisions, implementation workflow, known failure modes, and fixes that shaped this Train Booking Rate Limiting project.

---

## 1. Product Idea

The project models a high-demand train ticket booking platform, similar to Tatkal-style booking windows where thousands of users attempt to reserve seats within a very short time. The core challenge is not only accepting high traffic, but accepting it safely without creating unfair access, database overload, duplicate bookings, or double-booked seats.

The guiding principle is:

> Traffic-control layers decide **how many requests reach the database**. The database and booking transaction decide **what is allowed to happen correctly**.

That separation matters. Even a perfectly designed queue cannot protect the business if the final seat-booking transaction is unsafe. Likewise, a correct database transaction can still collapse if every user is allowed to hit it at once.

---

## 2. Original Problem Statement

The system needed to support:

- 10,000+ concurrent booking attempts.
- Protection against bot bursts and abusive clients.
- Fair admission during peak traffic.
- Strict prevention of double booking.
- Temporary seat holds with automatic expiry.
- Idempotent booking calls to avoid duplicate booking/payment behavior.
- Kafka only for asynchronous side effects, not as a queue for rejected bookings.
- Observability through metrics and dashboard artifacts.
- A frontend that demonstrates queue status, seat selection, hold countdown, and booking.

---

## 3. High-Level Architecture

```text
User / Browser
  ↓
Layer 0: Edge / WAF / Nginx throttling
  ↓
Layer 1: Token Bucket rate limiting
  ↓
Virtual Waiting Room: Redis sorted-set queue + admission token
  ↓
Layer 2: Adaptive concurrency gate
  ↓
Booking Service transaction
  ↓
Database row lock + seat lifecycle validation
  ↓
Kafka booking-confirmed event
  ↓
Notification / analytics consumers
```

The architecture intentionally uses multiple smaller controls instead of one giant bottleneck. Each layer solves a specific problem:

| Layer | Main purpose | Failure prevented |
| --- | --- | --- |
| Edge throttling | Stop abusive IP/client bursts before origin | Origin saturation |
| Token bucket | Apply per-user/API-key fairness | One user flooding booking endpoint |
| Waiting room | Admit only a controlled number of users | Thundering herd at booking time |
| Adaptive concurrency | Dynamically protect app/DB under latency/error stress | Cascading failure |
| DB transaction | Guarantee correctness | Double booking |
| Kafka | Async side effects after success | Slow notifications blocking booking |

---

## 4. Repository Build-Up: What Was Created

### 4.1 Backend foundation

A Spring Boot backend was created with a clean Controller → Service → Repository layout:

- Controllers expose booking, hold-seat, and queue-status APIs.
- Services contain booking, queue, rate-limit, and hold-release logic.
- Repositories isolate database access.
- DTOs keep API payloads separate from persistence entities.
- Exceptions are centralized through a global handler.

### 4.2 Traffic control components

Three layers of request control were added:

1. **Layer 0 edge guidance** in documentation.
2. **Layer 1 token bucket** in application code using Redis.
3. **Layer 2 adaptive concurrency** using an AIMD-style controller.

### 4.3 Virtual waiting room

A Redis sorted set (`booking_queue`) is used to model queue order. Users are admitted in batches by a scheduler. Admitted users receive short-lived queue tokens, so they cannot simply skip the queue and call the booking endpoint directly.

### 4.4 Booking domain

The seat model was upgraded from a basic booked flag into a real lifecycle:

```text
AVAILABLE → HELD → BOOKED
```

This avoids a major real-world failure mode: seats being permanently blocked if a user leaves midway.

### 4.5 Database schema

A production-grade MySQL schema was added with:

- users
- trains
- seats
- bookings
- idempotency records
- constraints and indexes
- sample dataset
- stored procedure for atomic booking with deadlock retry guidance

### 4.6 Kafka integration

Kafka is used only after successful booking to publish a `booking-confirmed` event. This preserves the rule that rejected booking requests should not be hidden inside Kafka for later processing, because that would break real-time consistency and user fairness.

### 4.7 Frontend

A Next.js frontend was scaffolded with:

- virtual waiting room panel
- queue polling
- Three.js 3D seat map
- hold-seat action
- hold countdown
- book-ticket action
- Zustand state management
- React Query API integration

---

## 5. Detailed Backend Workflow

### 5.1 User enters booking flow

The user selects a train and seat in the frontend. The frontend keeps local session state such as user ID, selected seat, user tier, queue token, and hold expiry.

### 5.2 Rate limiting runs before booking

When the user calls `/api/book-ticket`, the rate-limit filter resolves an identity from headers/API key/IP and consumes a token from Redis. If tokens are exhausted, the request fails early with HTTP 429.

This prevents a single user or bot from repeatedly hammering the booking endpoint.

### 5.3 Waiting room admission

The booking service enqueues users into Redis if they are not already queued. A scheduler releases a configured number of users per interval. When a released user polls queue status, the service returns an admission token with a short TTL.

That token is required during booking. This is important because the queue is not just informational; it is an admission-control mechanism.

### 5.4 Adaptive concurrency check

After rate limiting and before deep business processing, the adaptive concurrency filter checks the current in-flight request count. If the system is healthy, the concurrency limit increases gradually. If latency or errors rise, the limit decreases multiplicatively.

This behavior is inspired by TCP congestion control and protects downstream systems when latency indicates saturation.

### 5.5 Seat hold

The user must hold a seat before final booking. The hold operation locks the seat row pessimistically, validates seat ownership/status, and writes:

```text
status = HELD
heldByUserId = current user
holdExpiresAt = now + 5 minutes
```

If another user already holds the seat and the hold has not expired, the request fails.

### 5.6 Final booking transaction

The booking transaction checks:

- user has queue admission
- queue token is valid
- idempotency key has not already produced a booking
- seat exists
- seat is not already booked
- seat is held by this same user
- hold has not expired

Then it updates the seat to `BOOKED`, clears hold fields, inserts a booking row, stores idempotency information, consumes the queue token, and publishes Kafka confirmation.

### 5.7 Kafka side effect

Kafka is intentionally after success. The booking path remains synchronous and strongly consistent; Kafka handles only secondary workflows such as notifications and analytics.

---

## 6. Database Correctness Strategy

The database is the final authority for seat state. The most important correctness tools are:

- row-level locking (`SELECT ... FOR UPDATE` / pessimistic JPA lock)
- unique constraint on booked seat
- seat status lifecycle
- hold expiry field
- idempotency key mapping
- short transactions

### Why row locks matter

If User A and User B try to book the same seat:

1. User A obtains the row lock.
2. User B waits.
3. User A books and commits.
4. User B reads the updated row and sees the seat is already booked.
5. User B fails safely.

This prevents double booking even if traffic controls accidentally admit too many requests.

---

## 7. Frontend Workflow

The frontend demonstrates the entire product path:

1. User enters/selects user ID, train ID, and tier.
2. Queue panel polls `/queue-status`.
3. 3D seat map renders seats using color states:
   - green = available
   - yellow = held
   - red = booked
4. User clicks a seat in the 3D map.
5. User calls hold seat.
6. Countdown timer displays remaining hold time.
7. User books ticket after queue admission token is available.

The frontend is intentionally modular so later improvements can replace the local demo seat list with a real backend seat-availability endpoint.

---

## 8. Failures Discovered and Fixes Applied

### 8.1 Missing seat expiry

**Failure:** A seat could become blocked forever if a user abandoned booking.

**Fix:** Introduced `SeatStatus`, `heldByUserId`, `holdExpiresAt`, hold-seat API, and a scheduler to release expired holds.

### 8.2 Queue skipping risk

**Failure:** A user could theoretically call the booking endpoint directly without respecting queue order.

**Fix:** Added queue admission state and short-lived queue tokens. Booking validates the queue token before proceeding.

### 8.3 Lack of priority support

**Failure:** FIFO alone could not model premium/business priority users.

**Fix:** Redis sorted-set score can subtract a priority weight for premium users while preserving FIFO behavior inside tiers.

### 8.4 Cascading failure risk

**Failure:** If the database became slow, the application could continue accepting too much work.

**Fix:** Added adaptive concurrency and a circuit breaker to reject quickly under downstream instability.

### 8.5 Duplicate booking/payment risk

**Failure:** Client retries could create duplicate side effects.

**Fix:** Added idempotency records. Repeating the same idempotency key returns the same booking result.

### 8.6 Maven dependency resolution issue

**Failure:** Maven Central returned HTTP 403 in the execution environment during earlier validation attempts.

**Fix:** The project version was adjusted as requested, and the issue was identified as an environment/network-policy problem rather than an application design issue. The recommended validation path is to run Maven in an environment with Maven Central access or with an approved internal artifact mirror.

### 8.7 README quality gap

**Failure:** The first README described features but was not polished enough for professional handoff.

**Fix:** Rewrote README into a structured architecture and operations guide, then added this deep-dive document for full design reasoning.

---

## 9. Complete Request Flow

### 9.1 Queue status polling

```text
Frontend
  -> GET /queue-status?userId=u-123
  -> QueueController
  -> QueueService
  -> Redis ZSET / admitted token lookup
  -> returns position, ETA, admitted flag, queue token when admitted
```

### 9.2 Hold seat

```text
Frontend
  -> POST /api/hold-seat
  -> SeatController
  -> BookingService.holdSeat
  -> SeatRepository.findSeatForUpdate
  -> validate train + status + hold ownership
  -> write HELD + hold expiry
  -> return hold expiry to frontend
```

### 9.3 Book ticket

```text
Frontend
  -> POST /api/book-ticket
  -> Layer1RateLimitFilter
  -> AdaptiveConcurrencyFilter
  -> BookingController
  -> BookingService.createBooking
  -> QueueService admission/token validation
  -> Idempotency lookup
  -> SeatRepository row lock
  -> validate HELD by same user and not expired
  -> update seat BOOKED
  -> insert booking
  -> insert idempotency record
  -> consume queue token
  -> publish Kafka booking-confirmed event
  -> return booking response
```

---

## 10. Configuration and Tuning Philosophy

Most operational knobs live in `application.yml`:

- token bucket capacity/refill
- waiting room release rate
- waiting room max size
- adaptive concurrency min/max limit
- latency threshold
- circuit breaker window and failure threshold
- hold-release scheduler interval

For production, these values should be tuned from load-test data rather than guessed. A safe rollout usually starts conservatively, observes queue growth and database latency, then increases release rate and concurrency limits gradually.

---

## 11. Observability Story

Metrics were added so operators can answer:

- How large is the queue?
- How many booking requests are in flight?
- What is the adaptive concurrency limit right now?
- Are bookings succeeding or failing?
- Is latency increasing before errors appear?

The Grafana dashboard template turns those signals into an operational view.

---

## 12. Why Kafka Is Not Used for Rejected Bookings

Kafka is excellent for asynchronous workflows, but not for deciding who gets a scarce seat in real time. If rejected booking attempts were placed into Kafka for later processing, the system could accidentally book seats for users who were no longer admitted, whose holds expired, or who already saw a failure.

Therefore Kafka is used only after the synchronous booking transaction commits successfully.

---

## 13. Production Deployment Guidance

A realistic production deployment would include:

- CDN/WAF/Nginx at the edge.
- Dedicated API gateway with Redis-backed request rate limiter.
- Multiple Spring Boot application instances.
- Redis cluster or managed Redis for queues/tokens.
- Primary relational database for booking writes.
- Read replicas for non-critical read views.
- Kafka cluster for notifications and analytics.
- Prometheus + Grafana monitoring.
- Centralized logs and traces.

---

## 14. Remaining Improvements

The project is intentionally strong as a reference implementation, but production systems can go further:

- Add a backend seat-availability API for real seat map synchronization.
- Add WebSocket/SSE queue updates instead of polling.
- Store idempotency response as structured JSON in application entity and align JPA with the MySQL script.
- Add Testcontainers integration tests for Redis, Kafka, and MySQL.
- Add CI pipeline with Maven and frontend checks.
- Add OpenTelemetry tracing.
- Add chaos/load-test scripts for queue and booking saturation.

---

## 15. Final Mental Model

This project is built around one simple idea:

> Let many users arrive, let fewer users enter, let even fewer users hit the database concurrently, and let the database decide correctness with strict locks and constraints.

That is why the system combines rate limiting, queue admission, adaptive concurrency, seat holds, idempotency, and database transactions instead of relying on any single mechanism.
