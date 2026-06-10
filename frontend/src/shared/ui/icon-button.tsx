import { cn } from "@/shared/lib";
import type { ComponentProps, FC } from "react";

export type IconButtonProps = Omit<ComponentProps<"button">, "aria-label"> & {
  className?: string;
  Icon: FC<ComponentProps<"svg">>;
  "aria-label": string;
};

export function IconButton({ className, Icon, type = "button", ...props }: IconButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex size-9 cursor-pointer items-center justify-center rounded-full bg-surface-soft p-xs text-ink transition-colors outline-none hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary active:bg-surface-pressed disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      type={type}
      {...props}
    >
      <Icon className="pointer-events-none size-4.5" />
    </button>
  );
}
