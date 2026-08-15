# Train Booking Rate Limiting System

A production-oriented Train Booking platform designed for Tatkal-scale traffic spikes. This repository contains a resilient Spring Boot backend, a modern Next.js frontend, database scripts, and infrastructure references to enforce fairness, prevent overload, and preserve booking consistency under high concurrency.

---

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Core Capabilities](#core-capabilities)
- [Repository Structure](#repository-structure)
- [Backend Stack](#backend-stack)
- [Frontend Stack](#frontend-stack)
- [Booking Lifecycle](#booking-lifecycle)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Run Locally](#run-locally)
- [Observability](#observability)
- [Database](#database)
- [Security & Reliability Notes](#security--reliability-notes)
- [Future Improvements](#future-improvements)
- [Deep Dive: Design Decisions, Workflow, and Lessons](#deep-dive-design-decisions-workflow-and-lessons)

---

## Overview

This system applies **layered admission control** before database writes:
1. Edge throttling (CDN/WAF/Nginx)
2. Per-user token bucket rate limiting
3. Virtual waiting room with queue admission token
4. Adaptive concurrency protection on critical booking endpoint
5. Transaction-safe seat hold and booking with idempotency

The result is a safer path from user request to confirmed booking—even during sudden flash traffic.

---

## Architecture

```text
Client
  -> Edge (Cloudflare/Nginx throttling)
  -> API Gateway / App Layer Token Bucket
  -> Virtual Waiting Room (Redis ZSET)
  -> Adaptive Concurrency Gate (AIMD)
  -> Booking Transaction (MySQL/PostgreSQL + row lock)
  -> Kafka (success side-effects only)
```

### Traffic Layers
- **Layer-0 (Edge):** protects origin from abusive bursts and bot floods.
- **Layer-1 (Rate Limit):** per-user/API-key token bucket for request shaping.
- **Virtual Waiting Room:** queue-based admission with short-lived queue token.
- **Layer-2 (Adaptive Concurrency):** dynamic in-flight limit to prevent cascading failure.

---

## Core Capabilities

- Seat lifecycle: `AVAILABLE -> HELD -> BOOKED`
- Auto-release of expired holds
- Idempotent booking submission (`idempotencyKey` replay safety)
- Redis-backed queue with premium-priority support
- Circuit breaker on booking path
- Kafka publishing only after successful booking
- Micrometer metrics + Grafana dashboard template
- Optional dedicated Spring Cloud Gateway config

---

## Repository Structure

```text
.
├── src/main/java/com/example/trainbooking   # Spring Boot backend
├── src/main/resources/application.yml       # runtime config
├── src/test                                 # test configuration
├── frontend/                                # Next.js App Router frontend
├── database/mysql/                          # schema + sample data + operational docs
├── observability/grafana/                   # dashboard template
├── gateway/                                 # gateway reference config
├── EDGE_THROTTLING.md                       # Layer-0 edge guidance
└── README.md
```

---

## Backend Stack

- Java 21 + Spring Boot
- Spring Data JPA
- Redis (rate limit + waiting room)
- Kafka (async confirmation events)
- Resilience4j (circuit breaker)
- Micrometer + Prometheus endpoint

---

## Frontend Stack

- Next.js (App Router)
- TypeScript
- Tailwind CSS
- shadcn-style UI primitives
- Three.js seat visualization
- Zustand + React Query

Frontend details: see [`frontend/README.md`](frontend/README.md).

---

## Booking Lifecycle

1. User selects seat (frontend)
2. User enters queue and polls queue status
3. System grants short-lived queue token on admission
4. User calls **hold-seat** (temporary lock, e.g., 5 min)
5. User calls **book-ticket** with queue token + idempotency key
6. Backend validates hold ownership + expiry and books atomically
7. Booking confirmation event is emitted to Kafka

---

## API Reference

### 1) Hold Seat
`POST /api/hold-seat`

```json
{
  "userId": "u-123",
  "seatId": 1001,
  "trainId": "12951"
}
```

### 2) Queue Status
`GET /queue-status?userId=u-123`

```json
{
  "userId": "u-123",
  "position": 11,
  "estimatedWaitSeconds": 2,
  "admitted": false,
  "queueToken": null,
  "tokenExpiresInSeconds": 0
}
```

### 3) Book Ticket
`POST /api/book-ticket`

```json
{
  "userId": "u-123",
  "seatId": 1001,
  "trainId": "12951",
  "idempotencyKey": "5db364e4-cd36-4c70-b13d-809ecf",
  "queueToken": "0f9d...",
  "userTier": "PREMIUM"
}
```

**Important:** rejected booking attempts are not published to Kafka.

---

## Configuration

Primary runtime configuration is in:
- `src/main/resources/application.yml`

Includes:
- datasource + JPA settings
- Redis and Kafka settings
- traffic-control tuning
- circuit breaker thresholds
- management/metrics exposure

---

## Run Locally

### Backend prerequisites
- Java 21
- Maven
- Redis
- Kafka
- MySQL/PostgreSQL (based on configured profile)

### Start backend
```bash
mvn -U clean install
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Set frontend backend URL when needed:
```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

## Observability

- Prometheus metrics exposed via Spring Actuator
- Grafana dashboard template:
  - `observability/grafana/train-booking-dashboard.json`

Key signals:
- queue size
- adaptive concurrency limit
- in-flight requests
- booking success/failure rate
- API latency distributions

---

## Database

MySQL production artifacts:
- Schema + indexes + constraints + sample dataset + stored procedure:
  - `database/mysql/train_booking_mysql.sql`
- Read/write split strategy:
  - `database/mysql/READ_WRITE_SPLIT.md`

---

## Security & Reliability Notes

- Prefer running gateway + WAF in front of app nodes.
- Keep booking transactions short to reduce lock contention.
- Apply bounded retry only for deadlock/transient serialization conflicts.
- Enforce idempotency keys on client and server.
- Use dedicated secrets management for API keys and credentials.

---

## Future Improvements

- Real-time queue updates via WebSocket/SSE
- Distributed tracing with OpenTelemetry
- Canary rollout configuration and autoscaling policies
- Seat map availability API for live backend-driven 3D rendering

---

## Deep Dive: Design Decisions, Workflow, and Lessons

For a very detailed explanation of the project idea, architecture thought process, implementation workflow, failures discovered, and fixes applied, see [`docs/PROJECT_DEEP_DIVE.md`](docs/PROJECT_DEEP_DIVE.md).

---

## License

See [`LICENSE`](LICENSE).
