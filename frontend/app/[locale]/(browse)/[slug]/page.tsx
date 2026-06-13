import { CategoryPage } from "@/pages/category";
import { HomePage } from "@/pages/home";
import { SportPage } from "@/pages/sport";
import {
  DEFAULT_CATEGORY_NODE,
  HOME_CATEGORY_NODE,
  ROUTABLE_CATEGORY_NODES,
  STATIC_CATEGORY_SLUGS,
  type CategorySlug,
} from "@/shared/config";
import { locales } from "@/shared/i18n";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ locale: string; slug: CategorySlug }>;
};

export function generateStaticParams() {
  return locales.flatMap((locale) => {
    return STATIC_CATEGORY_SLUGS.map((slug) => ({ locale, slug }));
  });
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale, slug } = await params;
  if (slug === DEFAULT_CATEGORY_NODE.slug) {
    return { alternates: { canonical: `/${locale}` } };
  }
  return {};
}

export default async function Page({ params }: PageProps) {
  const { slug } = await params;

  if (!ROUTABLE_CATEGORY_NODES.some((node) => node.slug === slug)) {
    notFound();
  }

  const isSportTab = slug === "epl" || slug === "nba";
  if (isSportTab) {
    return <SportPage slug={slug} />;
  }

  const isHomeTab = HOME_CATEGORY_NODE.children?.some((child) => child.slug === slug);
  if (isHomeTab) {
    return <HomePage />;
  }

  return <CategoryPage categoryPrefix={slug} />;
}
