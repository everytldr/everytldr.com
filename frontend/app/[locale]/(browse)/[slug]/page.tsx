import { CategoryPage } from "@/pages/category";
import { DiscoverPage } from "@/pages/discover";
import { HomePage } from "@/pages/home";
import { SportPage } from "@/pages/sport";
import {
  DEFAULT_CATEGORY_NODE,
  HOME_CATEGORY_NODE,
  isFeedableCategory,
  isMainCategorySlug,
  ROUTABLE_CATEGORY_NODES,
  STATIC_CATEGORY_SLUGS,
  type CategorySlug,
} from "@/shared/config";
import { locales, type Locale } from "@/shared/i18n";
import { buildPageMetadata } from "@/shared/lib";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ locale: Locale; slug: CategorySlug }>;
};

export function generateStaticParams() {
  return locales.flatMap((locale) => {
    return STATIC_CATEGORY_SLUGS.map((slug) => ({ locale, slug }));
  });
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale, slug } = await params;

  if (!ROUTABLE_CATEGORY_NODES.some((node) => node.slug === slug)) {
    return {};
  }

  const isHomeTab = HOME_CATEGORY_NODE.children?.some((child) => child.slug === slug);
  if (isHomeTab) {
    const t = await getTranslations({ locale, namespace: "metadata.home" });
    return buildPageMetadata({
      title: t("title"),
      description: t("description"),
      locale,
      path: slug === DEFAULT_CATEGORY_NODE.slug ? "/" : `/${slug}`,
    });
  }

  const category = isMainCategorySlug(slug)
    ? (await getTranslations({ locale, namespace: "header.category" }))(slug)
    : (await getTranslations({ locale, namespace: "header.subcategory" }))(slug);

  const tMeta = await getTranslations({ locale, namespace: "metadata.category" });
  return buildPageMetadata({
    title: tMeta("title", { category }),
    description: tMeta("description", { category }),
    locale,
    path: `/${slug}`,
    feedSlug: isFeedableCategory(slug) ? slug : undefined,
  });
}

export default async function Page({ params }: PageProps) {
  const { locale, slug } = await params;

  if (!ROUTABLE_CATEGORY_NODES.some((node) => node.slug === slug)) {
    notFound();
  }

  const isSportTab = slug === "epl" || slug === "nba" || slug === "sport-events";
  if (isSportTab) {
    return <SportPage slug={slug} locale={locale} />;
  }

  if (slug === "discover") {
    return <DiscoverPage locale={locale} />;
  }

  if (slug === "latest") {
    return <CategoryPage locale={locale} />;
  }

  const isHomeTab = HOME_CATEGORY_NODE.children?.some((child) => child.slug === slug);
  if (isHomeTab) {
    return <HomePage />;
  }

  return <CategoryPage categoryPrefix={slug} locale={locale} />;
}
