import { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return <span className={cn("rounded-full bg-muted px-3 py-1 text-xs", className)} {...props} />;
}
