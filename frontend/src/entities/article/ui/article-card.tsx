"use client";

import type { ArticleListItem } from "@/shared/api";
import type { MainCategorySlug } from "@/shared/config";
import { cn, formatDate, markdownToPlainText } from "@/shared/lib";
import { Badge, Translation } from "@/shared/ui";
import { useLocale } from "next-intl";

type ArticleCardProps = {
  className?: string;
  titleClassName?: string;
  article: ArticleListItem;
};

export function ArticleCard({ className, titleClassName, article }: ArticleCardProps) {
  const locale = useLocale();
  const summary = markdownToPlainText(article.summary);
  const rootCategory = article.category.split("-")[0] as MainCategorySlug;

  return (
    <article
      className={cn(
        "flex min-w-0 flex-col gap-2xs border-b border-hairline-soft py-md last:border-b-0",
        className,
      )}
    >
      <Badge>
        <Translation tKey={`header.category.${rootCategory}`} />
      </Badge>
      <h3 className={cn("line-clamp-1 min-w-0 text-display-sm text-ink", titleClassName)}>
        {article.title}
      </h3>
      <p className="line-clamp-2 text-body-sm text-meta">{summary}</p>
      <p className="text-caption text-meta">
        {article.source} ·{" "}
        <time dateTime={article.publishedAt}>{formatDate(article.publishedAt, locale)}</time>
      </p>
    </article>
  );
}
