import ThemeProvider from "@/components/ThemeProvider";
import { NextIntlClientProvider } from "next-intl";
import { cookies } from "next/headers";
import { PropsWithChildren } from "react";
import { SyncedStorageProvider } from "synced-storage/react";

export default async function GlobalProvider({ children }: PropsWithChildren) {
  const cookieStore = await cookies();

  return (
    <SyncedStorageProvider ssrCookies={cookieStore.getAll()}>
      <ThemeProvider>
        <NextIntlClientProvider>{children}</NextIntlClientProvider>
      </ThemeProvider>
    </SyncedStorageProvider>
  );
}
