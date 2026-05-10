import { jetbrainsMono, pretendard } from "@/app/fonts";
import { GlobalProvider } from "@/app/providers";
import "@/app/styles";
import { routing } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Footer } from "@/widgets/footer";
import { Header } from "@/widgets/header";
import type { Metadata } from "next";
import { hasLocale } from "next-intl";
import { setRequestLocale } from "next-intl/server";
import { notFound } from "next/navigation";
import { type PropsWithChildren } from "react";

export const metadata: Metadata = {
  title: "everytldr",
  description: "Foreign news, summarised in your language.",
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
      </body>
    </html>
  );
}
