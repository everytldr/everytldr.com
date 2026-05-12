import { HomePage } from "@/pages/home";
import { SportPage } from "@/pages/sport";
import {
  DEFAULT_SUB_CATEGORY_SLUG,
  HOME_CATEGORY_NODE,
  SUB_CATEGORY_SLUGS,
  SubCategorySlug,
} from "@/shared/config";
import { locales } from "@/shared/i18n";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ locale: string; slug: SubCategorySlug }>;
};

export function generateStaticParams() {
  return locales.flatMap((locale) => {
    return SUB_CATEGORY_SLUGS.map((slug) => ({ locale, slug }));
  });
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale, slug } = await params;
  if (slug === DEFAULT_SUB_CATEGORY_SLUG) {
    return { alternates: { canonical: `/${locale}` } };
  }
  return {};
}

export default async function Page({ params }: PageProps) {
  const { slug } = await params;

  if (!SUB_CATEGORY_SLUGS.includes(slug)) {
    notFound();
  }

  if (slug === SubCategorySlug.EPL || slug === SubCategorySlug.NBA) {
    return <SportPage slug={slug} />;
  }

  if (HOME_CATEGORY_NODE.subs.includes(slug)) {
    return <HomePage />;
  }

  notFound();
}
