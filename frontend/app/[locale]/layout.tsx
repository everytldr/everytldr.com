import { jetbrainsMono, pretendard } from "@/app/fonts";
import { GlobalProvider } from "@/app/providers";
import "@/app/styles";
import { ADSENSE_CLIENT_ID } from "@/shared/config";
import { routing } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Footer } from "@/widgets/footer";
import { Header } from "@/widgets/header";
import type { Metadata } from "next";
import { hasLocale } from "next-intl";
import { setRequestLocale } from "next-intl/server";
import { notFound } from "next/navigation";
import Script from "next/script";
import { type PropsWithChildren } from "react";

export const metadata: Metadata = {
  title: "everytldr",
  description: "Foreign news, summarised in your language.",
  other: { "google-adsense-account": ADSENSE_CLIENT_ID },
};

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

type RootLayoutProps = PropsWithChildren<{
  params: Promise<{ locale: string }>;
}>;

export default async function RootLayout({ params, children }: RootLayoutProps) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) {
    notFound();
  }

  setRequestLocale(locale);

  return (
    <html
      className={cn(pretendard.variable, jetbrainsMono.variable)}
      lang={locale}
      suppressHydrationWarning
    >
      <body className="flex min-h-dvh flex-col">
        <GlobalProvider>
          <Header locale={locale} />
          <div className="flex-1">{children}</div>
          <Footer />
        </GlobalProvider>
        <Script
          src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_CLIENT_ID}`}
          crossOrigin="anonymous"
          strategy="afterInteractive"
        />
      </body>
    </html>
  );
}
