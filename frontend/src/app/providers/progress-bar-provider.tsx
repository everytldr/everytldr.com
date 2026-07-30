"use client";

import { useHydrated } from "@/shared/lib";
import { ProgressProvider } from "@bprogress/next/app";
import { type PropsWithChildren } from "react";

export function ProgressBarProvider({ children }: PropsWithChildren) {
  const hydrated = useHydrated();

  return (
    <>
      {hydrated && (
        <ProgressProvider
          color="var(--color-primary)"
          height="2px"
          options={{ showSpinner: false }}
        />
      )}
      {children}
    </>
  );
}
