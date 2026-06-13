import { SearchPage } from "@/pages/search";
import { type Locale, locales } from "@/shared/i18n";
import { buildPageMetadata } from "@/shared/lib";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

type PageProps = {
  params: Promise<{ locale: Locale }>;
  searchParams: Promise<{ q?: string }>;
};

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "metadata.search" });

  return buildPageMetadata({
    title: t("title"),
    description: t("description"),
    locale,
    path: "/search",
  });
}

export default async function Page({ searchParams }: PageProps) {
  const { q = "" } = await searchParams;
  return <SearchPage query={q} />;
}
