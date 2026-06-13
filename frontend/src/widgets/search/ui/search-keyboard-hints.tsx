import { cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { ArrowDown, ArrowUp, CornerDownLeft } from "lucide-react";
import type { ComponentProps } from "react";

type SearchKeyboardHintsProps = {
  className?: string;
};

export function SearchKeyboardHints({ className }: SearchKeyboardHintsProps) {
  return (
    <footer
      className={cn(
        "flex items-center justify-end gap-md border-t border-hairline-soft pt-md",
        className,
      )}
    >
      <span className="inline-flex items-center gap-2xs">
        <Kbd>
          <CornerDownLeft className="size-sm" />
        </Kbd>
        <Translation tKey="search.keyboard-hint.enter" />
      </span>
      <span className="inline-flex items-center gap-2xs">
        <span className="inline-flex items-center gap-px">
          <Kbd>
            <ArrowUp className="size-sm" />
          </Kbd>
          <Kbd>
            <ArrowDown className="size-sm" />
          </Kbd>
        </span>
        <Translation tKey="search.keyboard-hint.arrows" />
      </span>
      <span className="inline-flex items-center gap-2xs">
        <Kbd className="px-2xs text-micro">esc</Kbd>
        <Translation tKey="search.keyboard-hint.escape" />
      </span>
    </footer>
  );
}

type KbdProps = ComponentProps<"kbd">;

function Kbd({ className, children, ...props }: KbdProps) {
  return (
    <kbd
      className={cn(
        "inline-flex h-md min-w-md items-center justify-center rounded-xs bg-surface-soft text-micro text-ink dark:bg-surface-strong",
        className,
      )}
      {...props}
    >
      {children}
    </kbd>
  );
}
