"use client";

import { initBrowserMocks } from "@/shared/api";
import { useEffect, useState, type PropsWithChildren } from "react";

type MSWProviderProps = PropsWithChildren;

const isMockingEnabled =
  process.env.NODE_ENV === "development" && process.env.NEXT_PUBLIC_API_MOCKING !== "false";

export function MSWProvider({ children }: MSWProviderProps) {
  const [isReady, setIsReady] = useState(!isMockingEnabled);

  useEffect(() => {
    if (!isMockingEnabled) {
      return;
    }

    void initBrowserMocks().then(() => setIsReady(true));
  }, []);

  return isReady ? children : null;
}
