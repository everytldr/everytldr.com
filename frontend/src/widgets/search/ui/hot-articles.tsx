"use client";

import { getListPopularArticlesQueryKey, useListPopularArticles } from "@/shared/api";
import { cn } from "@/shared/lib";
import { Skeleton, Translation } from "@/shared/ui";
import { range } from "lodash-es";
import { CornerDownLeft } from "lucide-react";
import { useLocale } from "next-intl";

const HOT_ARTICLES_SIZE = 6;

type HotArticlesProps = {
  className?: string;
  onArticleSelect: (articleId: string) => void;
};

export function HotArticles({ className, onArticleSelect }: HotArticlesProps) {
  const locale = useLocale();
  const { data, isPending } = useListPopularArticles(
    { size: HOT_ARTICLES_SIZE },
    {
      query: {
        queryKey: [...getListPopularArticlesQueryKey({ size: HOT_ARTICLES_SIZE }), locale],
      },
      request: {
        headers: {
          "Accept-Language": locale,
        },
      },
    },
  );

  const articles = data?.status === 200 ? data.data.items : [];

  if (!isPending && articles.length === 0) {
    return null;
  }

  return (
    <section className={cn("flex flex-col gap-sm", className)}>
      <Translation className="text-title-md text-ink" as="h3" tKey="search.section.hot-articles" />
      <ul className="flex flex-col">
        {isPending
          ? range(HOT_ARTICLES_SIZE).map((index) => (
              <li key={index} className="flex items-center gap-md px-sm py-2xs">
                <Skeleton className="h-lh w-md shrink-0 text-caption-mono" />
                <Skeleton className="h-lh flex-1 text-body-md" />
              </li>
            ))
          : articles.map((article, index) => (
              <li key={article.id}>
                <HotArticleRow
                  rank={index + 1}
                  title={article.title}
                  onSelect={() => onArticleSelect(article.id)}
                />
              </li>
            ))}
      </ul>
    </section>
  );
}

type HotArticleRowProps = {
  className?: string;
  rank: number;
  title: string;
  onSelect: () => void;
};

function HotArticleRow({ className, rank, title, onSelect }: HotArticleRowProps) {
  return (
    <button
      className={cn(
        "group relative flex w-full cursor-pointer items-center gap-md rounded-sm px-sm py-2xs text-left transition-colors outline-none hover:bg-surface-soft focus-visible:z-10 focus-visible:bg-surface-soft focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong dark:hover:bg-surface-strong dark:focus-visible:bg-surface-strong dark:active:bg-surface-pressed",
        className,
      )}
      type="button"
      data-search-nav
      onClick={onSelect}
    >
      <span className="w-md shrink-0 text-caption-mono text-meta tabular-nums">{rank}</span>
      <span className="flex-1 truncate text-body-md text-ink">{title}</span>
      <CornerDownLeft className="size-sm shrink-0 text-meta-soft opacity-0 transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100" />
    </button>
  );
}
