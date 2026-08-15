"use client";

import { useEffect, useMemo, useState } from "react";

export function HoldCountdown({ holdExpiresAt }: { holdExpiresAt?: string }) {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const remaining = useMemo(() => {
    if (!holdExpiresAt) return 0;
    return Math.max(0, Math.floor((new Date(holdExpiresAt).getTime() - now) / 1000));
  }, [holdExpiresAt, now]);

  return <span className={remaining > 20 ? "text-emerald-400" : "text-amber-400"}>{remaining}s</span>;
}
