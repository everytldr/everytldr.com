"use client";

import { ProgressProvider } from "@bprogress/next/app";
import { type PropsWithChildren } from "react";

export function ProgressBarProvider({ children }: PropsWithChildren) {
  return (
    <ProgressProvider color="var(--color-primary)" height="2px" options={{ showSpinner: false }}>
      {children}
    </ProgressProvider>
  );
}
