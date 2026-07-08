import { SearchPage } from "@/pages/search";
import {
  getQueryClient,
  getSearchArticlesInfiniteQueryKey,
  getSearchArticlesSuspenseInfiniteQueryOptions,
} from "@/shared/api";
import { type Locale, locales } from "@/shared/i18n";
import { buildPageMetadata } from "@/shared/lib";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
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

  return {
    ...buildPageMetadata({
      title: t("title"),
      description: t("description"),
      locale,
      path: "/search",
    }),
    robots: { index: false, follow: true },
  };
}

export default async function Page({ params, searchParams }: PageProps) {
  const { locale } = await params;
  const { q = "" } = await searchParams;
  const trimmedQuery = q.trim();

  const queryClient = getQueryClient();
  if (trimmedQuery.length > 0) {
    await queryClient.prefetchInfiniteQuery(
      getSearchArticlesSuspenseInfiniteQueryOptions(
        { q: trimmedQuery },
        {
          query: {
            queryKey: [...getSearchArticlesInfiniteQueryKey({ q: trimmedQuery }), locale],
          },
          request: {
            headers: {
              "Accept-Language": locale,
            },
          },
        },
      ),
    );
  }

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <SearchPage query={q} />
    </HydrationBoundary>
  );
}
