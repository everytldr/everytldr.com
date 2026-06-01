"use client";

import { cn, ensure } from "@/shared/lib";
import { useEffect, useImperativeHandle, useRef, type ComponentProps } from "react";

export type TextareaProps = ComponentProps<"textarea">;

export function Textarea({ ref, className, value, defaultValue, ...props }: TextareaProps) {
  const innerRef = useRef<HTMLTextAreaElement>(null);

  useImperativeHandle(ref, () => ensure(innerRef.current, "Textarea ref is not set"));

  useEffect(() => {
    const textarea = innerRef.current;
    if (!textarea) {
      return;
    }

    textarea.style.height = "auto";
    textarea.style.height = `${textarea.scrollHeight}px`;
  }, [value, defaultValue]);

  return (
    <textarea
      ref={innerRef}
      className={cn(
        "w-full resize-none overflow-hidden rounded-md bg-canvas px-md py-sm text-body-md text-ink inset-ring-1 inset-ring-hairline-strong transition-all outline-none placeholder:text-meta hover:inset-ring-ink focus-visible:inset-ring-2 focus-visible:inset-ring-primary disabled:cursor-not-allowed disabled:bg-surface-strong disabled:text-meta-soft disabled:inset-ring-hairline aria-invalid:inset-ring-2 aria-invalid:inset-ring-semantic-error",
        className,
      )}
      value={value}
      defaultValue={defaultValue}
      data-slot="textarea"
      {...props}
    />
  );
}
