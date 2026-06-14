import { type ArticleDetailResponse } from "@/shared/api";
import { ADSENSE_SLOT_ARTICLE_DETAIL, SITE_URL } from "@/shared/config";
import { getPathname, type Locale } from "@/shared/i18n";
import {
  buildArticleDetailUrl,
  buildNewsArticleJsonLd,
  buildOgImageUrl,
  cn,
  formatDate,
  markdownToPlainText,
  serializeJsonLd,
} from "@/shared/lib";
import { AdSlot, Button, MarkdownContent, Translation } from "@/shared/ui";
import { ExternalLink } from "lucide-react";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { fetchArticleDetail } from "../api/fetch-article-detail";
import { ArticleComments, ArticleCommentsError, ArticleCommentsSkeleton } from "./article-comments";
import { ArticleLikeButton, ArticleLikeButtonSkeleton } from "./article-like-button";

type ArticleDetailPageProps = {
  className?: string;
  articleId: string;
  locale: Locale;
};

export async function ArticleDetailPage({ className, articleId, locale }: ArticleDetailPageProps) {
  const article = await fetchArticleDetail(articleId, locale);

  const jsonLd = buildNewsArticleJsonLd({
    url: `${SITE_URL}${getPathname({ locale, href: buildArticleDetailUrl(articleId) })}`,
    headline: article.title,
    description: markdownToPlainText(article.summary),
    datePublished: article.publishedAt,
    image: article.thumbnailUrl ? buildOgImageUrl(article.thumbnailUrl) : undefined,
  });

  return (
    <article className={cn("space-y-xl", className)}>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: serializeJsonLd(jsonLd) }}
      />

      <ArticleDetailContent article={article} locale={locale} />

      <div className="flex flex-wrap items-center gap-sm border-t border-hairline-soft pt-lg">
        <ErrorBoundary fallback={null}>
          <Suspense fallback={<ArticleLikeButtonSkeleton />}>
            <ArticleLikeButton articleId={articleId} />
          </Suspense>
        </ErrorBoundary>
        {article.contentUrl && (
          <Button variant="link" asChild>
            <a href={article.contentUrl} target="_blank" rel="noreferrer">
              <ExternalLink className="size-md" aria-hidden="true" />
              <Translation
                tKey="article-detail.source-link"
                values={{ source: article.source ?? "source" }}
              />
            </a>
          </Button>
        )}
      </div>

      <AdSlot className="w-full" slot={ADSENSE_SLOT_ARTICLE_DETAIL} />

      <ErrorBoundary fallback={<ArticleCommentsError />}>
        <Suspense fallback={<ArticleCommentsSkeleton />}>
          <ArticleComments articleId={articleId} locale={locale} />
        </Suspense>
      </ErrorBoundary>
    </article>
  );
}

type ArticleDetailContentProps = {
  className?: string;
  article: ArticleDetailResponse;
  locale: Locale;
};

function ArticleDetailContent({ className, article, locale }: ArticleDetailContentProps) {
  return (
    <div className={cn("space-y-lg", className)}>
      <header className="space-y-sm">
        <p className="text-caption text-meta">
          {article.source}
          {" · "}
          <time dateTime={article.publishedAt}>{formatDate(article.publishedAt, locale)}</time>
        </p>
        <h1 className="text-display-xl text-ink">{article.title}</h1>
      </header>

      {article.thumbnailUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          className="aspect-[16/9] w-full rounded-md bg-surface-soft object-cover"
          src={article.thumbnailUrl}
          alt=""
          decoding="async"
          fetchPriority="high"
          aria-hidden="true"
        />
      )}

      <MarkdownContent markdown={article.summary} />
    </div>
  );
}
