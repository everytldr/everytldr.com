import { ArticleList, ArticleListInfinite } from "@/entities/article";
import {
  getListArticlesInfiniteQueryKey,
  getListArticlesSuspenseInfiniteQueryOptions,
  getQueryClient,
  type ArticleListItem,
  type listArticlesResponse,
} from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { HydrationBoundary, dehydrate, type InfiniteData } from "@tanstack/react-query";
import { cacheLife, cacheTag } from "next/cache";
import { Suspense } from "react";

type CategoryPageProps = {
  className?: string;
  categoryPrefix: string;
  locale: Locale;
};

export async function CategoryPage({ className, categoryPrefix, locale }: CategoryPageProps) {
  const { articles, dehydratedState } = await prefetchArticles(categoryPrefix, locale);

  return (
    <main className={cn("py-lg", className)}>
      <Container className="space-y-sm">
        <HydrationBoundary state={dehydratedState}>
          <Suspense
            fallback={
              <ArticleList
                articles={articles}
                empty={
                  <Translation
                    className="rounded-md border border-hairline-soft bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
                    as="p"
                    tKey="category.empty-state"
                  />
                }
              />
            }
          >
            <ArticleListInfinite
              categoryPrefix={categoryPrefix}
              locale={locale}
              empty={
                <Translation
                  className="rounded-md border border-hairline-soft bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
                  as="p"
                  tKey="category.empty-state"
                />
              }
            />
          </Suspense>
        </HydrationBoundary>
      </Container>
    </main>
  );
}

async function prefetchArticles(categoryPrefix: string, locale: Locale) {
  "use cache";

  cacheLife("minutes");
  cacheTag(`articles:${locale}:${categoryPrefix}`);

  const queryClient = getQueryClient();
  const queryOptions = getListArticlesSuspenseInfiniteQueryOptions(
    { categoryPrefix },
    {
      query: {
        queryKey: [...getListArticlesInfiniteQueryKey({ categoryPrefix }), locale],
      },
      request: {
        headers: {
          "Accept-Language": locale,
        },
      },
    },
  );
  await queryClient.prefetchInfiniteQuery(queryOptions);

  const data = queryClient.getQueryData<InfiniteData<listArticlesResponse>>(queryOptions.queryKey);
  const articles: ArticleListItem[] =
    data?.pages.flatMap((page) => (page.status === 200 ? page.data.items : []) ?? []) ?? [];

  return { articles, dehydratedState: dehydrate(queryClient) };
}
