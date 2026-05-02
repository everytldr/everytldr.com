"use client";

import { assert } from "@/shared/lib/assert";
import { type Nullable, type Optional } from "@/shared/lib/nullable";
import { createContext, type ReactNode, useContext, useEffect, useMemo } from "react";
import { useStorageState } from "synced-storage/react";

export type Theme = "light" | "dark" | "system";
export const THEME_COOKIE_KEY = "theme";

type ThemeContextValue = {
  themeState: [Theme, React.Dispatch<React.SetStateAction<Theme>>];
};

const ThemeContext = createContext<Nullable<ThemeContextValue>>(null);

export function useTheme() {
  const context = useContext(ThemeContext);
  assert(context, "useTheme must be used within a ThemeProvider");
  return context;
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useStorageState<Theme>(THEME_COOKIE_KEY, "system");

  const value: ThemeContextValue = useMemo(
    () => ({ themeState: [theme, setTheme] }),
    [theme, setTheme],
  );

  useEffect(() => {
    const root = document.documentElement;

    const toggleDark = (isDark: Optional<boolean>) => {
      const next = typeof isDark === "boolean" ? isDark : !isDark;
      root.classList.toggle("dark", next);
    };

    if (theme === "system") {
      const handleThemeChange = (e: MediaQueryListEvent) => toggleDark(e.matches);
      const mq = window.matchMedia("(prefers-color-scheme: dark)");
      toggleDark(mq.matches);
      mq.addEventListener("change", handleThemeChange);

      return () => mq.removeEventListener("change", handleThemeChange);
    }

    toggleDark(theme === "dark");
  }, [theme]);

  return <ThemeContext value={value}>{children}</ThemeContext>;
}
