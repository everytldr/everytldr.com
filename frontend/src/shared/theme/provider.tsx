"use client";

import { ThemeProvider as NextThemesProvider, useTheme as useNextTheme } from "next-themes";
import { type Dispatch, type PropsWithChildren, type SetStateAction } from "react";

export type Theme = "light" | "dark" | "system";

export function ThemeProvider({ children }: PropsWithChildren) {
  return (
    <NextThemesProvider attribute="class" defaultTheme="system" enableSystem>
      {children}
    </NextThemesProvider>
  );
}

export function useTheme(): {
  theme: Theme;
  setTheme: Dispatch<SetStateAction<Theme>>;
} {
  const { theme, setTheme } = useNextTheme();
  return {
    theme: (theme ?? "system") as Theme,
    setTheme: setTheme as Dispatch<SetStateAction<Theme>>,
  };
}
