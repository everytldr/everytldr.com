import { SportPage } from "@/pages/sport";
import { EplTabSlug, EplTeam } from "@/shared/config";
import { type Locale, locales } from "@/shared/i18n";
import { buildPageMetadata } from "@/shared/lib";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<
    { locale: Locale } & (
      | { slug: "nba"; subSlug: unknown } // TODO: MVP 이후에 구현 예정
      | { slug: "epl"; subSlug: EplTabSlug | EplTeam }
    )
  >;
};

const EPL_SUB_SLUGS = [...Object.values(EplTabSlug), ...Object.values(EplTeam)];

export function generateStaticParams() {
  return locales.flatMap((locale) => {
    return EPL_SUB_SLUGS.map((subSlug) => ({
      locale,
      slug: "epl",
      subSlug,
    }));
  });
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const pageParams = await params;
  const { locale, slug, subSlug } = pageParams;

  if (slug !== "epl") {
    return {};
  }

  const tHeader = await getTranslations({ locale });
  const category = tHeader("header.subcategory.epl");

  const tMeta = await getTranslations({ locale, namespace: "metadata.category" });
  return buildPageMetadata({
    title: tMeta("title", { category }),
    description: tMeta("description", { category }),
    locale,
    path: `/epl/${subSlug}`,
    feedSlug: resolveFeedSlug(pageParams),
  });
}

function resolveFeedSlug(params: Awaited<PageProps["params"]>) {
  if (params.slug === "epl" || params.subSlug === EplTabSlug.News) {
    return "epl";
  }

  if (params.subSlug === EplTabSlug.News) {
    return undefined;
  }

  return `${params.slug}/${params.subSlug}`;
}

export default async function Page({ params: _params }: PageProps) {
  const params = await _params;

  if (params.slug !== "epl") {
    notFound();
  }

  return <SportPage {...params} />;
}
