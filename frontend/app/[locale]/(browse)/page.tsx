import { DiscoverPage } from "@/pages/discover";
import { type Locale, locales } from "@/shared/i18n";
import { buildPageMetadata, buildSiteJsonLd, serializeJsonLd } from "@/shared/lib";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

type PageProps = {
  params: Promise<{ locale: Locale }>;
};

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "metadata.home" });

  return buildPageMetadata({
    title: t("title"),
    description: t("description"),
    locale,
    path: "/",
  });
}

export default async function Page({ params }: PageProps) {
  const { locale } = await params;

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: serializeJsonLd(buildSiteJsonLd(locale)) }}
      />
      <DiscoverPage locale={locale} />
    </>
  );
}
