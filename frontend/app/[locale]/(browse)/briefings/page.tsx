import { BriefingsArchivePage } from "@/pages/briefings-archive";
import { locales, type Locale } from "@/shared/i18n";
import { buildPageMetadata } from "@/shared/lib";
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
  const t = await getTranslations({ locale, namespace: "metadata.briefings" });

  return buildPageMetadata({
    title: t("title"),
    description: t("description"),
    locale,
    path: "/briefings",
  });
}

export default async function Page({ params }: PageProps) {
  const { locale } = await params;

  return <BriefingsArchivePage locale={locale} />;
}
