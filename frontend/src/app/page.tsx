"use client";

import { BookingFlowPanel } from "@/components/booking-flow-panel";
import { QueueStatusPanel } from "@/components/queue-status-panel";
import { SeatMap3D } from "@/components/seat-map-3d";
import { Card } from "@/components/ui/card";
import { useBookingStore } from "@/store/booking-store";

export default function HomePage() {
  const { userId, trainId, userTier, setUser } = useBookingStore();

  return (
    <main className="mx-auto min-h-screen max-w-7xl space-y-6 p-6">
      <header className="space-y-2">
        <h1 className="text-3xl font-bold">Train Booking Command Center</h1>
        <p className="text-zinc-400">
          Next.js + Tailwind + shadcn/ui + Three.js frontend for waiting room and high-concurrency booking.
        </p>
      </header>

      <Card className="grid gap-3 md:grid-cols-3">
        <label className="text-sm">
          User ID
          <input
            className="mt-1 w-full rounded border border-border bg-background px-3 py-2"
            value={userId}
            onChange={(e) => setUser(e.target.value, trainId, userTier)}
          />
        </label>
        <label className="text-sm">
          Train ID
          <input
            className="mt-1 w-full rounded border border-border bg-background px-3 py-2"
            value={trainId}
            onChange={(e) => setUser(userId, e.target.value, userTier)}
          />
        </label>
        <label className="text-sm">
          Tier
          <select
            className="mt-1 w-full rounded border border-border bg-background px-3 py-2"
            value={userTier}
            onChange={(e) => setUser(userId, trainId, e.target.value as "NORMAL" | "PREMIUM")}
          >
            <option value="NORMAL">NORMAL</option>
            <option value="PREMIUM">PREMIUM</option>
          </select>
        </label>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <SeatMap3D />
        <div className="space-y-6">
          <QueueStatusPanel />
          <BookingFlowPanel />
        </div>
      </div>
    </main>
  );
}
