"use client";

import { ThemeProvider as NextThemesProvider, useTheme as useNextTheme } from "next-themes";
import { type PropsWithChildren } from "react";

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
  return [parseTheme(theme), setTheme];
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
