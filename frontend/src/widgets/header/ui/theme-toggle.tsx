"use client";

import { useTheme, type Theme } from "@/shared/theme";
import { IconButton } from "@/shared/ui";
import { Monitor, Moon, Sun } from "lucide-react";
import { useTranslations } from "next-intl";
import { useMemo, type ComponentProps, type FC } from "react";

type ThemeToggleProps = {
  className?: string;
};

export function ThemeToggle({ className }: ThemeToggleProps) {
  const t = useTranslations("header");
  const [theme, setTheme] = useTheme();
  const state = useMemo(() => getThemeState(theme), [theme]);

  return (
    <IconButton
      className={className}
      Icon={state.Icon}
      aria-label={`${t("theme")}: ${t(state.tKey)}`}
      onClick={() => setTheme(state.nextTheme)}
    />
  );
}

function getThemeState(theme: Theme): {
  Icon: FC<ComponentProps<"svg">>;
  tKey: "theme-system" | "theme-light" | "theme-dark";
  nextTheme: Theme;
} {
  switch (theme) {
    case "system":
      return { Icon: Monitor, tKey: "theme-system", nextTheme: "light" };
    case "light":
      return { Icon: Sun, tKey: "theme-light", nextTheme: "dark" };
    case "dark":
      return { Icon: Moon, tKey: "theme-dark", nextTheme: "system" };
    default:
      return { Icon: Monitor, tKey: "theme-system", nextTheme: "light" };
  }
}
