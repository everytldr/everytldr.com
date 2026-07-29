import { ArticleScrollRow, ArticleScrollRowSkeleton } from "@/entities/article";
import { type Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Skeleton, Translation } from "@/shared/ui";
import { fetchRelatedArticles } from "../api/fetch-related-articles";

const RELATED_SECTION_SIZE = 10;

type RelatedArticlesProps = {
  className?: string;
  articleId: string;
  locale: Locale;
};

export async function RelatedArticles({ className, articleId, locale }: RelatedArticlesProps) {
  const articles = await fetchRelatedArticles(articleId, locale, RELATED_SECTION_SIZE);

  if (articles.length === 0) {
    return null;
  }

  return (
    <section className={cn("space-y-sm", className)}>
      <Translation className="text-display-md text-ink" as="h2" tKey="article-detail.related" />
      <ArticleScrollRow articles={articles} locale={locale} />
    </section>
  );
}

type RelatedArticlesSkeletonProps = {
  className?: string;
};

export function RelatedArticlesSkeleton({ className }: RelatedArticlesSkeletonProps) {
  return (
    <div className={cn("space-y-sm border-t border-hairline-soft pt-lg", className)}>
      <Skeleton className="w-24 text-display-md">&nbsp;</Skeleton>
      <ArticleScrollRowSkeleton count={RELATED_SECTION_SIZE} />
    </div>
  );
}
