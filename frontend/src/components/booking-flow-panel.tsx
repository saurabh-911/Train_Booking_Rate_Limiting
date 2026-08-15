"use client";

import { useMutation } from "@tanstack/react-query";
import { bookTicket, holdSeat } from "@/lib/api";
import { useBookingStore } from "@/store/booking-store";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { HoldCountdown } from "@/components/hold-countdown";

function createIdempotencyKey(userId: string, seatId: number) {
  return `${userId}-${seatId}-${Date.now()}`;
}

export function BookingFlowPanel() {
  const {
    userId,
    trainId,
    userTier,
    selectedSeatId,
    selectedSeatNumber,
    queueToken,
    holdExpiresAt,
    setHoldExpiresAt,
    updateSeatStatus
  } = useBookingStore();

  const holdMutation = useMutation({
    mutationFn: () => holdSeat({ userId, seatId: selectedSeatId!, trainId }),
    onSuccess: (data) => {
      updateSeatStatus(data.seatId, "HELD");
      setHoldExpiresAt(data.holdExpiresAt);
    }
  });

  const bookMutation = useMutation({
    mutationFn: () =>
      bookTicket({
        userId,
        seatId: selectedSeatId!,
        trainId,
        idempotencyKey: createIdempotencyKey(userId, selectedSeatId!),
        queueToken,
        userTier
      }),
    onSuccess: () => {
      updateSeatStatus(selectedSeatId!, "BOOKED");
      setHoldExpiresAt(undefined);
    }
  });

  return (
    <Card className="space-y-4">
      <h2 className="text-lg font-semibold">Booking Flow</h2>
      <div className="text-sm text-zinc-300">
        <p>User: {userId}</p>
        <p>Train: {trainId}</p>
        <p>Seat: {selectedSeatNumber ?? "Select from 3D map"}</p>
        <p>Queue token: {queueToken ? "Ready" : "Not available"}</p>
        <p>
          Hold timer: <HoldCountdown holdExpiresAt={holdExpiresAt} />
        </p>
      </div>

      <div className="flex gap-3">
        <Button disabled={!selectedSeatId || holdMutation.isPending} onClick={() => holdMutation.mutate()}>
          {holdMutation.isPending ? "Holding..." : "1) Hold Seat"}
        </Button>
        <Button
          variant="outline"
          disabled={!selectedSeatId || !queueToken || bookMutation.isPending}
          onClick={() => bookMutation.mutate()}
        >
          {bookMutation.isPending ? "Booking..." : "2) Book Ticket"}
        </Button>
      </div>

      {holdMutation.error && <p className="text-sm text-red-400">{(holdMutation.error as Error).message}</p>}
      {bookMutation.error && <p className="text-sm text-red-400">{(bookMutation.error as Error).message}</p>}
      {bookMutation.data && <p className="text-sm text-emerald-400">{bookMutation.data.message}</p>}
    </Card>
  );
}
