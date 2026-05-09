import { ThemeProvider } from "@/shared/theme";
import { NextIntlClientProvider } from "next-intl";
import { cookies } from "next/headers";
import { type PropsWithChildren } from "react";
import { SyncedStorageProvider } from "synced-storage/react";
import { QueryProvider } from "./query-provider";

export async function GlobalProvider({ children }: PropsWithChildren) {
  const cookieStore = await cookies();

  return (
    <SyncedStorageProvider ssrCookies={cookieStore.getAll()}>
      <ThemeProvider>
        <NextIntlClientProvider>
          <QueryProvider>{children}</QueryProvider>
        </NextIntlClientProvider>
      </ThemeProvider>
    </SyncedStorageProvider>
  );
}
