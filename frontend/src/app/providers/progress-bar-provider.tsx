"use client";

import { ProgressProvider } from "@bprogress/next/app";
import { Suspense, type PropsWithChildren } from "react";

export function ProgressBarProvider({ children }: PropsWithChildren) {
  return (
    <>
      <Suspense>
        <ProgressProvider
          color="var(--color-primary)"
          height="2px"
          options={{ showSpinner: false }}
        />
      </Suspense>
      {children}
    </>
  );
}
