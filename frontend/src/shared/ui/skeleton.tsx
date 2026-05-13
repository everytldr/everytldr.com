import { cn } from "@/shared/lib";
import type { ComponentProps } from "react";

type SkeletonProps = ComponentProps<"div">;

export function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <div
      className={cn("animate-pulse rounded-xs bg-surface-strong", className)}
      data-slot="skeleton"
      {...props}
    />
  );
}
