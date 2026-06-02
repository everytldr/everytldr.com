import { ThemeProvider } from "@/shared/theme";
import { NextIntlClientProvider } from "next-intl";
import { type PropsWithChildren } from "react";
import { SyncedStorageProvider } from "synced-storage/react";
import { MSWProvider } from "./msw-provider";
import { QueryProvider } from "./query-provider";

export function GlobalProvider({ children }: PropsWithChildren) {
  return (
    <SyncedStorageProvider>
      <ThemeProvider>
        <NextIntlClientProvider>
          <MSWProvider>
            <QueryProvider>{children}</QueryProvider>
          </MSWProvider>
        </NextIntlClientProvider>
      </ThemeProvider>
    </SyncedStorageProvider>
  );
}
