"use client";

import type { ArticleListItem } from "@/shared/api";
import { cn } from "@/shared/lib";
import { useFormatter } from "next-intl";

type ArticleCardProps = {
  className?: string;
  article: ArticleListItem;
};

export function ArticleCard({ className, article }: ArticleCardProps) {
  const format = useFormatter();
  const relativeTime = format.relativeTime(new Date(article.publishedAt));

  return (
    <article
      className={cn(
        "flex items-start gap-md border-b border-hairline-soft py-md last:border-b-0",
        className,
      )}
    >
      <div className="flex min-w-0 flex-1 flex-col gap-2xs">
        <h3 className="line-clamp-2 min-w-0 text-display-sm text-ink">{article.title}</h3>
        <p className="line-clamp-1 text-body-sm text-meta">{article.summary}</p>
        <p className="text-caption text-meta">
          {article.source} · {relativeTime}
        </p>
      </div>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        className="size-24 shrink-0 rounded-md bg-surface-soft object-cover"
        src={article.thumbnailUrl}
        alt=""
        loading="lazy"
        decoding="async"
        aria-hidden="true"
      />
    </article>
  );
}
