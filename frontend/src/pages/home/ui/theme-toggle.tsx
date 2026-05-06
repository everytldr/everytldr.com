"use client";

import { useTheme, type Theme } from "@/shared/theme";
import { Monitor, Moon, Sun } from "lucide-react";
import { useMemo, type ComponentProps, type FC } from "react";

export function ThemeToggle() {
  const {
    themeState: [theme, setTheme],
  } = useTheme();
  const state = useMemo(() => getThemeState(theme), [theme]);

  return (
    <button
      className="hidden size-9 items-center justify-center rounded-full bg-surface-strong text-ink transition-colors hover:bg-surface-soft md:inline-flex"
      type="button"
      aria-label={state.ariaLabel}
      onClick={() => setTheme(state.nextTheme)}
    >
      <state.Icon className="size-4" />
    </button>
  );
}

function getThemeState(theme: Theme): {
  Icon: FC<ComponentProps<"svg">>;
  ariaLabel: string;
  nextTheme: Theme;
} {
  switch (theme) {
    case "system":
      return { Icon: Monitor, ariaLabel: "Switch to light theme", nextTheme: "light" };
    case "light":
      return { Icon: Sun, ariaLabel: "Switch to dark theme", nextTheme: "dark" };
    case "dark":
      return { Icon: Moon, ariaLabel: "Switch to auto theme", nextTheme: "system" };
    default:
      return { Icon: Monitor, ariaLabel: "Switch to light theme", nextTheme: "light" };
  }
}
