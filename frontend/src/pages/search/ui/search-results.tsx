"use client";

import { ArticleCardSkeleton, ArticleList } from "@/entities/article";
import {
  getSearchArticlesInfiniteQueryKey,
  getSearchArticlesSuspenseInfiniteQueryOptions,
  type ArticleListItem,
  type searchArticlesResponse,
} from "@/shared/api";
import type { Optional } from "@/shared/lib";
import { useSuspenseInfiniteQuery } from "@tanstack/react-query";
import { range } from "lodash-es";
import { useLocale } from "next-intl";
import type { ReactNode } from "react";
import { InView } from "react-intersection-observer";

type SearchResultsProps = {
  className?: string;
  query: string;
  empty: ReactNode;
};

export function SearchResults({ className, query, empty }: SearchResultsProps) {
  const locale = useLocale();
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = useSuspenseInfiniteQuery({
    ...getSearchArticlesSuspenseInfiniteQueryOptions(
      { q: query },
      {
        query: {
          queryKey: [...getSearchArticlesInfiniteQueryKey({ q: query }), locale],
        },
        request: {
          headers: {
            "Accept-Language": locale,
          },
        },
      },
    ),
    getNextPageParam: (lastPage: searchArticlesResponse): Optional<number> =>
      lastPage.status === 200 ? (lastPage.data.nextOffset ?? undefined) : undefined,
  });

  const items: ArticleListItem[] = data.pages.flatMap((page) =>
    page.status === 200 ? (page.data.items ?? []) : [],
  );

  return (
    <div className={className}>
      <ArticleList articles={items} empty={empty}>
        {isFetchingNextPage &&
          range(2).map((i) => (
            <li key={i}>
              <ArticleCardSkeleton />
            </li>
          ))}
      </ArticleList>
      {hasNextPage && (
        <InView as="div" rootMargin="200px" aria-hidden="true" onChange={handleSentinelChange} />
      )}
    </div>
  );

  function handleSentinelChange(inView: boolean) {
    if (inView && !isFetchingNextPage) {
      fetchNextPage();
    }
  }
}

type SearchResultsSkeletonProps = {
  className?: string;
};

export function SearchResultsSkeleton({ className }: SearchResultsSkeletonProps) {
  return (
    <ul className={className}>
      {range(4).map((i) => (
        <li key={i}>
          <ArticleCardSkeleton />
        </li>
      ))}
    </ul>
  );
}
