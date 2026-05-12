"use client";

import { cn, ensure } from "@/shared/lib";
import { Search, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useImperativeHandle, useRef, type ComponentProps, type ReactNode } from "react";

export type InputProps = Omit<ComponentProps<"input">, "placeholder"> & {
  placeholder?: ReactNode;
  variant?: "default" | "search";
};

export function Input({
  ref,
  className,
  placeholder,
  type,
  variant = "default",
  ...props
}: InputProps) {
  const innerRef = useRef<HTMLInputElement>(null);
  const t = useTranslations("common.aria-label");

  useImperativeHandle(ref, () => ensure(innerRef.current, "Input ref is not set"));

  return (
    <div className={cn("relative h-11", className)}>
      <input
        ref={innerRef}
        className={cn(
          "peer size-full py-sm text-body-md text-ink ring-1 transition-all outline-none ring-inset not-placeholder-shown:pr-11 focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed aria-invalid:ring-2 aria-invalid:ring-semantic-error",
          "[&::-webkit-search-cancel-button]:appearance-none", // NOTE: webkit clear button
          variant === "default" &&
            "rounded-sm bg-canvas px-md ring-hairline-strong hover:ring-ink disabled:bg-surface-strong disabled:text-meta-soft disabled:ring-hairline",
          variant === "search" &&
            "rounded-full bg-surface-soft pr-md pl-11 ring-hairline hover:bg-surface-strong focus-visible:bg-canvas disabled:opacity-50",
        )}
        placeholder=" " // NOTE: space, not "" — empty placeholder won't trigger `:placeholder-shown`
        type={type}
        {...props}
      />
      {variant === "search" && (
        <Search className="pointer-events-none absolute top-1/2 left-md size-md -translate-y-1/2 text-meta" />
      )}
      <button
        className="absolute top-1/2 right-sm inline-flex size-9 -translate-y-1/2 items-center justify-center rounded-full text-meta transition-colors outline-none peer-placeholder-shown:invisible hover:text-ink focus-visible:ring-2 focus-visible:ring-primary active:text-ink"
        type="button"
        aria-label={t("clear")}
        onClick={handleClear}
      >
        <X className="size-md" />
      </button>
      {placeholder && (
        <div
          className={cn(
            "pointer-events-none absolute inset-y-0 flex items-center gap-xs truncate text-body-md text-meta peer-not-placeholder-shown:invisible peer-focus:invisible",
            variant === "default" && "right-md left-md",
            variant === "search" && "right-md left-11",
          )}
        >
          {placeholder}
        </div>
      )}
    </div>
  );

  function handleClear() {
    const input = innerRef.current;
    if (!input) {
      return;
    }

    // NOTE: write through the native setter so React's value tracker stays stale — otherwise onChange won't fire.
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;
    setter?.call(input, "");
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.focus();
  }
}
