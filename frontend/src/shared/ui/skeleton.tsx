import { cn } from "@/shared/lib";
import type { ComponentProps } from "react";

type SkeletonProps = ComponentProps<"span">;

export function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <span
      className={cn("block animate-pulse rounded-xs bg-surface-strong", className)}
      data-slot="skeleton"
      {...props}
    />
  );
}
