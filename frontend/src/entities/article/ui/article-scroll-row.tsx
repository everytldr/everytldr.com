import type { ArticleListItem } from "@/shared/api";
import type { LeafCategorySlug } from "@/shared/config";
import { Link, type Locale } from "@/shared/i18n";
import { buildArticleDetailUrl, cn, formatDate } from "@/shared/lib";
import { Badge, ScrollableRow, Translation } from "@/shared/ui";

type ArticleScrollRowProps = {
  className?: string;
  articles: ArticleListItem[];
  locale: Locale;
};

export function ArticleScrollRow({ className, articles, locale }: ArticleScrollRowProps) {
  return (
    <ScrollableRow className={className} scrollerClassName="px-md -mx-md" scrollStep="item" fade>
      <ul className="flex items-stretch gap-sm">
        {articles.map((article) => (
          <li key={article.id} className="w-56 shrink-0 sm:w-64">
            <Link
              className="group block h-full outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas"
              href={buildArticleDetailUrl(article.id)}
              prefetch={false}
            >
              <ArticleScrollCard article={article} locale={locale} />
            </Link>
          </li>
        ))}
      </ul>
    </ScrollableRow>
  );
}

type ArticleScrollCardProps = {
  className?: string;
  article: ArticleListItem;
  locale: Locale;
};

function ArticleScrollCard({ className, article, locale }: ArticleScrollCardProps) {
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
