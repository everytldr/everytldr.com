import { SportPage } from "@/pages/sport";
import { EplTeam, SubCategorySlug } from "@/shared/config";
import { locales } from "@/shared/i18n";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<
    { locale: string } & (
      | { slug: SubCategorySlug.NBA; filter?: unknown } // TODO: MVP 이후에 구현 예정
      | { slug: SubCategorySlug.EPL; filter?: EplTeam }
    )
  >;
};

const EPL_FILTERS = Object.values(EplTeam);

export function generateStaticParams() {
  return locales.flatMap((locale) => {
    return EPL_FILTERS.map((filter) => ({
      locale,
      slug: SubCategorySlug.EPL,
      filter,
    }));
  });
}

export default async function Page({ params: _params }: PageProps) {
  const params = await _params;

  if (params.slug === SubCategorySlug.EPL) {
    return <SportPage categorySlug={params.slug} filter={params.filter} />;
  }

  notFound();
}
