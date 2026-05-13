"use client";

import { getQueryClient } from "@/shared/api";
import { QueryClientProvider } from "@tanstack/react-query";
import { type PropsWithChildren } from "react";

export function QueryProvider({ children }: PropsWithChildren) {
  const client = getQueryClient();
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
