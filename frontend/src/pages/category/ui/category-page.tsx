import { ArticleCardSkeleton, ArticleListInfinite } from "@/entities/article";
import {
  getListArticlesInfiniteQueryKey,
  getListArticlesSuspenseInfiniteQueryOptions,
  getQueryClient,
} from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { cn, type Optional } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { range } from "lodash-es";
import { cacheLife, cacheTag } from "next/cache";
import { connection } from "next/server";
import { Suspense } from "react";

type CategoryPageProps = {
  className?: string;
  categoryPrefix?: string;
  locale: Locale;
};

export function CategoryPage({ className, categoryPrefix, locale }: CategoryPageProps) {
  return (
    <main className={cn("py-lg", className)}>
      <Container className="space-y-sm">
        <Suspense
          fallback={range(6).map((i) => (
            <ArticleCardSkeleton key={i} />
          ))}
        >
          <CategoryArticles categoryPrefix={categoryPrefix} locale={locale} />
        </Suspense>
      </Container>
    </main>
  );
}

type CategoryArticlesProps = {
  className?: string;
  categoryPrefix?: string;
  locale: Locale;
};

async function CategoryArticles({ className, categoryPrefix, locale }: CategoryArticlesProps) {
  await connection();

  const dehydratedState = await prefetchArticles(categoryPrefix, locale);

  return (
    <HydrationBoundary state={dehydratedState}>
      <ArticleListInfinite
        className={className}
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
    </HydrationBoundary>
  );
}

async function prefetchArticles(categoryPrefix: Optional<string>, locale: Locale) {
  "use cache";

  cacheLife("minutes");
  cacheTag(`articles:${locale}:${categoryPrefix ?? "latest"}`);

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

  return dehydrate(queryClient);
}
