import { SportPage } from "@/pages/sport";
import { EplTabSlug, EplTeam } from "@/shared/config";
import { locales } from "@/shared/i18n";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<
    { locale: string } & (
      | { slug: "nba"; subSlug?: unknown } // TODO: MVP 이후에 구현 예정
      | { slug: "epl"; subSlug?: EplTabSlug | EplTeam }
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

export default async function Page({ params: _params }: PageProps) {
  const params = await _params;

  if (params.slug !== "epl") {
    notFound();
  }

  return <SportPage slug={params.slug} subSlug={params.subSlug} />;
}
