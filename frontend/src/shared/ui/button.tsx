import { cn } from "@/shared/lib";
import { Slot } from "radix-ui";
import type { ComponentProps } from "react";

type ButtonProps = ComponentProps<"button"> & {
  variant: "ghost" | "link" | "primary" | "secondary";
  asChild?: boolean;
};

export function Button({ className, variant, asChild, ...props }: ButtonProps) {
  const Comp = asChild ? Slot.Root : "button";

  return (
    <Comp
      className={cn(
        "inline-flex shrink-0 cursor-pointer items-center justify-center gap-xs whitespace-nowrap transition-colors outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas disabled:cursor-not-allowed",
        variant === "primary" &&
          "h-11 rounded-sm bg-primary px-md text-button-md text-on-primary hover:bg-primary-hover active:bg-primary-pressed disabled:bg-primary-disabled",
        variant === "secondary" &&
          "h-11 rounded-sm border border-hairline-strong bg-canvas px-md text-button-md text-ink hover:bg-surface-soft active:bg-surface-strong disabled:opacity-50 dark:hover:bg-surface-strong dark:active:bg-surface-pressed",
        variant === "ghost" &&
          "h-auto rounded-sm bg-transparent px-md py-2 text-button-md text-ink hover:bg-surface-soft active:bg-surface-strong disabled:opacity-50 dark:hover:bg-surface-strong dark:active:bg-surface-pressed",
        variant === "link" &&
          "h-auto bg-transparent p-0 text-body-sm text-primary underline-offset-4 hover:text-primary-hover hover:underline active:text-primary-pressed disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}
