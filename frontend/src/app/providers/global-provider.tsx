import { ThemeProvider } from "@/shared/theme";
import { NextIntlClientProvider } from "next-intl";
import { type PropsWithChildren } from "react";
import { QueryProvider } from "./query-provider";

export function GlobalProvider({ children }: PropsWithChildren) {
  return (
    <ThemeProvider>
      <NextIntlClientProvider>
        <QueryProvider>{children}</QueryProvider>
      </NextIntlClientProvider>
    </ThemeProvider>
  );
}
