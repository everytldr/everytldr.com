"use client";

import type { ArticleListItem } from "@/shared/api";
import type { LeafCategorySlug } from "@/shared/config";
import { cn, markdownToPlainText, useHydrated } from "@/shared/lib";
import { Badge, RelativeTime, Skeleton, Translation } from "@/shared/ui";

type ArticleCardProps = {
  className?: string;
  titleClassName?: string;
  article: ArticleListItem;
};

export function ArticleCard({ className, titleClassName, article }: ArticleCardProps) {
  const hydrated = useHydrated();
  const summary = markdownToPlainText(article.summary);
  const category = article.category.replace(/^([^-]+-[^-]+)-.*/, "$1") as LeafCategorySlug;

  return (
    <article className={cn("flex min-w-0 flex-col gap-2xs py-md", className)}>
      <Badge>
        <Translation tKey={`header.subcategory.${category}`} />
      </Badge>
      <h3 className={cn("line-clamp-1 min-w-0 text-display-sm text-ink", titleClassName)}>
        {article.title}
      </h3>
      <p className="line-clamp-2 text-body-sm text-meta">{summary}</p>
      <p className="text-caption text-meta">
        {article.source} ·{" "}
        <time dateTime={article.publishedAt}>
          {hydrated ? (
            <RelativeTime date={article.publishedAt} />
          ) : (
            <Skeleton className="inline-block w-12 align-middle">&nbsp;</Skeleton>
          )}
        </time>
      </p>
    </article>
  );
}
