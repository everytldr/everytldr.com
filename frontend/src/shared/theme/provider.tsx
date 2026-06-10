"use client";

import { ThemeProvider as NextThemesProvider, useTheme as useNextTheme } from "next-themes";
import { type PropsWithChildren, useSyncExternalStore } from "react";

export type Theme = "light" | "dark" | "system";

export function ThemeProvider({ children }: PropsWithChildren) {
  return (
    <NextThemesProvider attribute="class" defaultTheme="system" enableSystem>
      {children}
    </NextThemesProvider>
  );
}

export function useTheme(): [Theme, (theme: Theme) => void] {
  const { theme, setTheme } = useNextTheme();
  const hydrated = useHydrated();
  return [hydrated ? parseTheme(theme) : "system", setTheme];
}

function useHydrated(): boolean {
  return useSyncExternalStore(
    () => () => {},
    () => true,
    () => false,
  );
}

function parseTheme(value: string | undefined): Theme {
  switch (value) {
    case "light":
    case "dark":
    case "system":
      return value;
    default:
      return "system";
  }
}
