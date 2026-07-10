import { jetbrainsMono, pretendard } from "@/app/fonts";
import { GlobalProvider } from "@/app/providers";
import "@/app/styles";
import { ADSENSE_CLIENT_ID, GA_MEASUREMENT_ID, SITE_URL, SITE_VERIFICATION } from "@/shared/config";
import { routing } from "@/shared/i18n";
import { buildPageMetadata, cn } from "@/shared/lib";
import { Footer } from "@/widgets/footer";
import { Header } from "@/widgets/header";
import type { Metadata } from "next";
import { hasLocale } from "next-intl";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { notFound } from "next/navigation";
import Script from "next/script";
import { type PropsWithChildren, ViewTransition } from "react";

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

type RootLayoutProps = PropsWithChildren<{
  params: Promise<{ locale: string }>;
}>;

export async function generateMetadata({
  params,
}: Pick<RootLayoutProps, "params">): Promise<Metadata> {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) {
    return {};
  }

  const t = await getTranslations({ locale, namespace: "metadata.default" });

  return {
    ...buildPageMetadata({
      title: t("title"),
      description: t("description"),
      locale,
    }),
    metadataBase: new URL(SITE_URL),
    other: {
      "google-adsense-account": ADSENSE_CLIENT_ID,
      "naver-site-verification": SITE_VERIFICATION.naver,
      "msvalidate.01": SITE_VERIFICATION.bing,
    },
  };
}

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
          <ViewTransition>
            <div className="flex-1">{children}</div>
          </ViewTransition>
          <Footer />
        </GlobalProvider>
        {process.env.NODE_ENV === "production" && (
          <>
            <Script
              src={`https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`}
              strategy="afterInteractive"
            />
            <Script strategy="afterInteractive" id="google-analytics">
              {`
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());
              gtag('config', '${GA_MEASUREMENT_ID}');
            `}
            </Script>
            <Script
              src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_CLIENT_ID}`}
              crossOrigin="anonymous"
              strategy="afterInteractive"
            />
          </>
        )}
      </body>
    </html>
  );
}
