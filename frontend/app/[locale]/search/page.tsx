import { SearchPage } from "@/pages/search";
import { locales } from "@/shared/i18n";

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

type PageProps = {
  searchParams: Promise<{ q?: string }>;
};

export default async function Page({ searchParams }: PageProps) {
  const { q = "" } = await searchParams;
  return <SearchPage query={q} />;
}
