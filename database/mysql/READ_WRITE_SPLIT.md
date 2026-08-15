# Read / Write split strategy (production)

## Topology
- **Primary (write DB):** all booking mutations (`hold-seat`, `book-ticket`, idempotency writes, seat release jobs).
- **Read replicas:** search APIs (`seat availability`, train listing, booking history dashboards).

## Routing rules
1. **Strong consistency endpoints** (booking path) always hit primary:
   - `SELECT ... FOR UPDATE` on `seats`
   - `UPDATE seats ...`
   - `INSERT bookings ...`
   - `INSERT idempotency_records ...`
2. **Eventually consistent endpoints** can read replica:
   - availability search
   - analytics/reports

## Spring Boot implementation pattern
- Use `AbstractRoutingDataSource` with context key `READ`/`WRITE`.
- Default route = WRITE.
- Annotate read-only service methods with `@Transactional(readOnly = true)` + aspect sets route `READ`.
- Booking transaction methods force route `WRITE`.

## Safety guardrails
- Never use replica for idempotency lookup in booking critical path (replica lag can create duplicates).
- Keep transaction windows short (lock row -> validate -> update -> insert -> commit).
- Retry deadlock/serialization failures with exponential backoff + jitter in application layer.

## Deadlock retry recommendation
- Retries: 2-3 attempts.
- Retry only for SQLState `40001` and MySQL error `1213`.
- Backoff: 25ms, 75ms, 150ms (+ jitter).
