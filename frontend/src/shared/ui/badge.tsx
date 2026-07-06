import { cn } from "@/shared/lib";
import type { PropsWithChildren } from "react";

type BadgeProps = PropsWithChildren<{
  className?: string;
}>;

export function Badge({ className, children }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex w-fit items-center rounded-xs bg-tint-gray px-2xs text-micro text-tint-gray-fg",
        className,
      )}
    >
      {children}
    </span>
  );
}
