import type { ArticleListItem } from "@/shared/api";
import type { LeafCategorySlug } from "@/shared/config";
import { Link, type Locale } from "@/shared/i18n";
import { buildArticleDetailUrl, cn, formatDate } from "@/shared/lib";
import { Badge, ScrollableRow, Skeleton, Translation } from "@/shared/ui";
import { range } from "lodash-es";
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
      <ScrollableRow scrollerClassName="px-md -mx-md" scrollStep="item" fade>
        <ul className="flex items-stretch gap-sm">
          {articles.map((article) => (
            <li key={article.id} className="w-56 shrink-0 sm:w-64">
              <Link
                className="group block h-full outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas"
                href={buildArticleDetailUrl(article.id)}
                prefetch={false}
              >
                <RelatedArticleCard article={article} locale={locale} />
              </Link>
            </li>
          ))}
        </ul>
      </ScrollableRow>
    </section>
  );
}

type RelatedArticleCardProps = {
  className?: string;
  article: ArticleListItem;
  locale: Locale;
};

function RelatedArticleCard({ className, article, locale }: RelatedArticleCardProps) {
  const category = article.category.replace(/^([^-]+-[^-]+)-.*/, "$1") as LeafCategorySlug;

  return (
    <article
      className={cn(
        "flex h-full min-w-0 flex-col gap-2xs rounded-md border border-hairline bg-canvas p-md dark:bg-surface-soft",
        className,
      )}
    >
      <Badge>
        <Translation tKey={`header.subcategory.${category}`} />
      </Badge>
      <h3 className="line-clamp-3 min-w-0 text-title-md text-ink group-hover:text-primary">
        {article.title}
      </h3>
      <p className="mt-auto text-caption text-meta">
        {article.source} ·{" "}
        <time dateTime={article.publishedAt}>{formatDate(article.publishedAt, locale)}</time>
      </p>
    </article>
  );
}

type RelatedArticlesSkeletonProps = {
  className?: string;
};

export function RelatedArticlesSkeleton({ className }: RelatedArticlesSkeletonProps) {
  return (
    <div className={cn("space-y-sm border-t border-hairline-soft pt-lg", className)}>
      <Skeleton className="w-24 text-display-md">&nbsp;</Skeleton>
      <div className="-mx-md pc:-mx-xl">
        <div className="scrollbar-hidden overflow-x-auto px-md pc:px-xl">
          <ul className="flex items-stretch gap-sm">
            {range(RELATED_SECTION_SIZE).map((i) => (
              <li key={i} className="w-56 shrink-0 sm:w-64">
                <div className="flex h-full min-w-0 flex-col gap-2xs rounded-md border border-hairline bg-canvas p-md dark:bg-surface-soft">
                  <Skeleton className="w-16 rounded-xs text-micro">&nbsp;</Skeleton>
                  <div className="flex flex-col">
                    <Skeleton className="w-full text-title-md">&nbsp;</Skeleton>
                    <Skeleton className="w-full text-title-md">&nbsp;</Skeleton>
                    <Skeleton className="w-2/3 text-title-md">&nbsp;</Skeleton>
                  </div>
                  <Skeleton className="mt-auto w-1/2 text-caption">&nbsp;</Skeleton>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
