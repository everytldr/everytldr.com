import { SportPage } from "@/pages/sport";
import { EplPageTab, EplTeam, SubCategorySlug } from "@/shared/config";
import { locales } from "@/shared/i18n";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<
    { locale: string } & (
      | { slug: SubCategorySlug.NBA; subSlug?: unknown } // TODO: MVP 이후에 구현 예정
      | { slug: SubCategorySlug.EPL; subSlug?: EplPageTab | EplTeam }
    )
  >;
};

const EPL_SUB_SLUGS = [...Object.values(EplPageTab), ...Object.values(EplTeam)];

export function generateStaticParams() {
  return locales.flatMap((locale) => {
    return EPL_SUB_SLUGS.map((subSlug) => ({
      locale,
      slug: SubCategorySlug.EPL,
      subSlug,
    }));
  });
}

export default async function Page({ params: _params }: PageProps) {
  const params = await _params;

  if (params.slug === SubCategorySlug.EPL) {
    return <SportPage slug={params.slug} subSlug={params.subSlug} />;
  }

  notFound();
}
