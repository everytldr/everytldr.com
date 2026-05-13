"use client";

import {
  getListArticlesSuspenseInfiniteQueryOptions,
  type ArticleListItem,
  type listArticlesResponse,
} from "@/shared/api";
import type { Optional } from "@/shared/lib";
import { useSuspenseInfiniteQuery } from "@tanstack/react-query";
import { range } from "lodash-es";
import { useEffect, type ReactNode } from "react";
import { useInView } from "react-intersection-observer";
import { ArticleCard } from "./article-card";
import { ArticleCardSkeleton } from "./article-card-skeleton";

type ArticleListInfiniteProps = {
  className?: string;
  categoryPrefix: string;
  empty: ReactNode;
};

export function ArticleListInfinite({
  className,
  categoryPrefix,
  empty,
}: ArticleListInfiniteProps) {
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = useSuspenseInfiniteQuery({
    ...getListArticlesSuspenseInfiniteQueryOptions({ categoryPrefix }),
    getNextPageParam: (lastPage: listArticlesResponse): Optional<string> =>
      lastPage.data.nextCursor,
  });
  const { ref: sentinelRef, inView } = useInView({ rootMargin: "200px" });

  useEffect(() => {
    if (inView && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [inView, hasNextPage, isFetchingNextPage, fetchNextPage]);

  const items: ArticleListItem[] = data.pages.flatMap((page) => page.data.items ?? []);

  return (
    <div className={className}>
      {items.length === 0 ? (
        empty
      ) : (
        <>
          <ul>
            {items.map((article) => (
              <li key={article.id}>
                <ArticleCard article={article} />
              </li>
            ))}
            {isFetchingNextPage &&
              range(2).map((i) => (
                <li key={i}>
                  <ArticleCardSkeleton />
                </li>
              ))}
          </ul>
          {hasNextPage && <div ref={sentinelRef} aria-hidden="true" />}
        </>
      )}
    </div>
  );
}
