import { ArticleList, ArticleListInfinite } from "@/entities/article";
import {
  getListArticlesInfiniteQueryKey,
  getListArticlesSuspenseInfiniteQueryOptions,
  getQueryClient,
  type ArticleListItem,
  type listArticlesResponse,
} from "@/shared/api";
import { type EplTeam } from "@/shared/config";
import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { HydrationBoundary, dehydrate, type InfiniteData } from "@tanstack/react-query";
import { cacheLife, cacheTag } from "next/cache";
import { Suspense } from "react";
import { EplTeamFilter } from "./epl-team-filter";

type EplNewsTabProps = {
  className?: string;
  filter?: EplTeam;
  locale: Locale;
};

export async function EplNewsTab({ className, filter, locale }: EplNewsTabProps) {
  const categoryPrefix = filter ? `sport-football-epl-${filter}` : "sport-football-epl";
  const { articles, dehydratedState } = await prefetchEplArticles(categoryPrefix, locale);

  return (
    <div className={cn("space-y-sm", className)}>
      <EplTeamFilter filter={filter} />
      <section>
        <HydrationBoundary state={dehydratedState}>
          <Suspense
            fallback={
              <ArticleList
                articles={articles}
                empty={
                  <Translation
                    className="rounded-md border border-hairline-soft bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
                    as="p"
                    tKey="epl.news.empty-state"
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
                  tKey="epl.news.empty-state"
                />
              }
            />
          </Suspense>
        </HydrationBoundary>
      </section>
    </div>
  );
}

async function prefetchEplArticles(categoryPrefix: string, locale: Locale) {
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
