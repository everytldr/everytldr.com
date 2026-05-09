import { HomePage } from "@/pages/home";
import { SUB_CATEGORY_SLUGS, SubCategorySlug } from "@/shared/config";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ locale: string; slug: SubCategorySlug }>;
};

export function generateStaticParams() {
  return SUB_CATEGORY_SLUGS.map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale, slug } = await params;
  if (slug === SubCategorySlug.Discover) {
    return { alternates: { canonical: `/${locale}` } };
  }
  return {};
}

export default async function Page({ params }: PageProps) {
  const { slug } = await params;

  if (!SUB_CATEGORY_SLUGS.includes(slug)) {
    notFound();
  }

  return <HomePage categorySlug={slug} />;
}
