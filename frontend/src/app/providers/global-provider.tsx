import { ThemeProvider } from "@/shared/theme";
import { Toaster } from "@/shared/ui";
import { NextIntlClientProvider } from "next-intl";
import { type PropsWithChildren } from "react";
import { SyncedStorageProvider } from "synced-storage/react";
import { MSWProvider } from "./msw-provider";
import { ProgressBarProvider } from "./progress-bar-provider";
import { QueryProvider } from "./query-provider";

export function GlobalProvider({ children }: PropsWithChildren) {
  return (
    <SyncedStorageProvider>
      <ThemeProvider>
        <NextIntlClientProvider>
          <MSWProvider>
            <QueryProvider>
              <ProgressBarProvider>
                {children}
                <Toaster />
              </ProgressBarProvider>
            </QueryProvider>
          </MSWProvider>
        </NextIntlClientProvider>
      </ThemeProvider>
    </SyncedStorageProvider>
  );
}
