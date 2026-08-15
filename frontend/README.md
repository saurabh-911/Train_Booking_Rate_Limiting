# Train Booking Frontend (Next.js)

## Stack
- Next.js App Router
- Tailwind CSS
- shadcn-style UI primitives
- Three.js seat map
- Zustand + React Query

## Features
- Virtual waiting room panel polling `/queue-status`
- 3D seat rendering (green/available, yellow/held, red/booked)
- Booking flow:
  1. `/api/hold-seat`
  2. Countdown timer for hold expiry
  3. `/api/book-ticket`

## Run
```bash
cd frontend
npm install
npm run dev
```

Set backend URL via:
```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```
