"use client";

import type { ArticleListItem } from "@/shared/api";
import { resolveLeafCategorySlug } from "@/shared/config";
import { cn, markdownToPlainText } from "@/shared/lib";
import { Badge, RelativeTime, Translation } from "@/shared/ui";

type ArticleCardProps = {
  className?: string;
  titleClassName?: string;
  article: ArticleListItem;
};

export function ArticleCard({ className, titleClassName, article }: ArticleCardProps) {
  const summary = markdownToPlainText(article.summary);
  const category = resolveLeafCategorySlug(article.category);

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
        {article.source} · <RelativeTime date={article.publishedAt} />
      </p>
    </article>
  );
}
