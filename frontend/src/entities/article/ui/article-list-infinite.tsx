"use client";

import {
  getListArticlesInfiniteQueryKey,
  getListArticlesSuspenseInfiniteQueryOptions,
  type ArticleListItem,
  type listArticlesResponse,
} from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import type { Optional } from "@/shared/lib";
import { useSuspenseInfiniteQuery } from "@tanstack/react-query";
import { range } from "lodash-es";
import { useEffect, type ReactNode } from "react";
import { useInView } from "react-intersection-observer";
import { ArticleCardSkeleton } from "./article-card-skeleton";
import { ArticleList } from "./article-list";

type ArticleListInfiniteProps = {
  className?: string;
  categoryPrefix?: string;
  empty: ReactNode;
  locale: Locale;
};

export function ArticleListInfinite({
  className,
  categoryPrefix,
  empty,
  locale,
}: ArticleListInfiniteProps) {
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = useSuspenseInfiniteQuery({
    ...getListArticlesSuspenseInfiniteQueryOptions(
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
    ),
    getNextPageParam: (lastPage: listArticlesResponse): Optional<string> =>
      lastPage.status === 200 ? (lastPage.data.nextCursor ?? undefined) : undefined,
  });
  const { ref: sentinelRef, inView } = useInView({ rootMargin: "200px" });

  useEffect(() => {
    if (inView && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [inView, hasNextPage, isFetchingNextPage, fetchNextPage]);

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
      {hasNextPage && <div ref={sentinelRef} aria-hidden="true" />}
    </div>
  );
}
