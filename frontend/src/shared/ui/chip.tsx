import { cn } from "@/shared/lib";
import { Slot } from "radix-ui";
import type { ComponentProps } from "react";

type ChipProps = ComponentProps<"button"> & {
  isSelected: boolean;
  asChild?: boolean;
};

export function Chip({ className, isSelected, asChild, ...props }: ChipProps) {
  const Comp = asChild ? Slot.Root : "button";

  return (
    <Comp
      className={cn(
        "inline-flex h-9 shrink-0 cursor-pointer items-center gap-xs rounded-full px-md text-button-sm whitespace-nowrap transition-colors outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas disabled:cursor-not-allowed disabled:opacity-50",
        isSelected
          ? "border border-transparent bg-ink text-on-ink active:bg-ink/90"
          : "border border-hairline bg-canvas text-body hover:bg-surface-soft hover:text-ink active:bg-surface-strong active:text-ink dark:hover:bg-surface-strong dark:active:bg-surface-pressed",
        className,
      )}
      {...props}
    />
  );
}
