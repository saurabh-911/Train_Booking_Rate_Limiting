"use client";

import { useQuery } from "@tanstack/react-query";
import { getQueueStatus } from "@/lib/api";
import { useBookingStore } from "@/store/booking-store";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useEffect } from "react";

export function QueueStatusPanel() {
  const userId = useBookingStore((s) => s.userId);
  const setQueueToken = useBookingStore((s) => s.setQueueToken);

  const { data, isFetching, error } = useQuery({
    queryKey: ["queue-status", userId],
    queryFn: () => getQueueStatus(userId),
    refetchInterval: 3000
  });

  useEffect(() => {
    if (data?.queueToken) {
      setQueueToken(data.queueToken);
    }
  }, [data?.queueToken, setQueueToken]);

  return (
    <Card className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Virtual Waiting Room</h2>
        <Badge>{isFetching ? "Polling..." : "Live"}</Badge>
      </div>
      {error && <p className="text-sm text-red-400">{(error as Error).message}</p>}
      <div className="grid grid-cols-2 gap-3 text-sm">
        <div>
          <p className="text-zinc-400">Position</p>
          <p className="text-xl font-bold">{data?.position ?? "--"}</p>
        </div>
        <div>
          <p className="text-zinc-400">ETA</p>
          <p className="text-xl font-bold">{data?.estimatedWaitSeconds ?? "--"}s</p>
        </div>
      </div>
      <p className={data?.admitted ? "text-emerald-400" : "text-amber-400"}>
        {data?.admitted ? "Admitted. You can book now." : "Waiting for admission."}
      </p>
    </Card>
  );
}
