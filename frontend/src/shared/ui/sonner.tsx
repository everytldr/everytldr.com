"use client";

import { cn } from "@/shared/lib";
import { useTheme } from "next-themes";
import type { CSSProperties } from "react";
import { Toaster as SonnerToaster, type ToasterProps as SonnerToasterProps } from "sonner";

type ToasterProps = SonnerToasterProps;

export function Toaster({ className, style, toastOptions, ...props }: ToasterProps) {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";

  return (
    <SonnerToaster
      className={cn("toaster group", className)}
      theme={isDark ? "dark" : "light"}
      position="bottom-center"
      richColors
      style={
        {
          "--normal-bg": isDark ? "var(--color-surface-soft)" : "var(--color-canvas)",
          "--normal-text": "var(--color-ink)",
          "--normal-border": "var(--color-hairline)",
          "--error-bg": isDark ? "var(--color-surface-soft)" : "var(--color-canvas)",
          "--error-text": "var(--color-semantic-error)",
          "--error-border": "var(--color-hairline)",
          "--border-radius": "var(--radius-md)",
          ...style,
        } as CSSProperties
      }
      toastOptions={{
        ...toastOptions,
        style: {
          boxShadow: "var(--shadow-floating)",
          fontSize: "var(--text-body-sm)",
          lineHeight: "var(--text-body-sm--line-height)",
          ...toastOptions?.style,
        },
      }}
      {...props}
    />
  );
}

export { toast } from "sonner";
