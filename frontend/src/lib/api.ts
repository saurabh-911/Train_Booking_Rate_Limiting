import { BookingResponse, HoldSeatResponse, QueueStatus } from "@/lib/types";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const payload = await res.json().catch(() => ({ error: `HTTP ${res.status}` }));
    throw new Error(payload.error ?? `HTTP ${res.status}`);
  }
  return res.json();
}

export async function getQueueStatus(userId: string): Promise<QueueStatus> {
  const res = await fetch(`${BASE_URL}/queue-status?userId=${encodeURIComponent(userId)}`, { cache: "no-store" });
  return handle<QueueStatus>(res);
}

export async function holdSeat(payload: { userId: string; seatId: number; trainId: string }) {
  const res = await fetch(`${BASE_URL}/api/hold-seat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return handle<HoldSeatResponse>(res);
}

export async function bookTicket(payload: {
  userId: string;
  seatId: number;
  trainId: string;
  idempotencyKey: string;
  queueToken?: string;
  userTier?: string;
}) {
  const res = await fetch(`${BASE_URL}/api/book-ticket`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-User-Id": payload.userId },
    body: JSON.stringify(payload)
  });
  return handle<BookingResponse>(res);
}
