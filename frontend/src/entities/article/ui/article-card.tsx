"use client";

import type { ArticleListItem } from "@/shared/api";
import { cn, formatDate, useHydrated } from "@/shared/lib";
import { RelativeTime } from "@/shared/ui";
import { useLocale } from "next-intl";

type ArticleCardProps = {
  className?: string;
  titleClassName?: string;
  article: ArticleListItem;
};

export function ArticleCard({ className, titleClassName, article }: ArticleCardProps) {
  const locale = useLocale();
  const hydrated = useHydrated();

  return (
    <article
      className={cn(
        "flex items-start gap-md border-b border-hairline-soft py-md last:border-b-0",
        className,
      )}
    >
      <div className="flex min-w-0 flex-1 flex-col gap-2xs">
        <h3 className={cn("line-clamp-2 min-w-0 text-display-sm text-ink", titleClassName)}>
          {article.title}
        </h3>
        <p className="line-clamp-1 text-body-sm text-meta">{article.summary}</p>
        <p className="text-caption text-meta">
          {article.source} ·{" "}
          <time dateTime={article.publishedAt}>
            {hydrated ? (
              <RelativeTime date={article.publishedAt} />
            ) : (
              formatDate(article.publishedAt, locale)
            )}
          </time>
        </p>
      </div>
      {article.thumbnailUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          className="size-24 shrink-0 rounded-md bg-surface-soft object-cover"
          src={article.thumbnailUrl}
          alt=""
          loading="lazy"
          decoding="async"
          aria-hidden="true"
        />
      )}
    </article>
  );
}
