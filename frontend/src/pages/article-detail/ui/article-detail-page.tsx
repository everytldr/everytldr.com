import { type ArticleDetailResponse } from "@/shared/api";
import { ADSENSE_SLOT_ARTICLE_DETAIL } from "@/shared/config";
import { cn, formatDate } from "@/shared/lib";
import { AdSlot, Button, Translation } from "@/shared/ui";
import { ExternalLink } from "lucide-react";
import { getLocale } from "next-intl/server";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { fetchArticleDetail } from "../api/fetch-article-detail";
import { ArticleComments, ArticleCommentsError, ArticleCommentsSkeleton } from "./article-comments";
import { ArticleLikeButton, ArticleLikeButtonSkeleton } from "./article-like-button";

type ArticleDetailPageProps = {
  className?: string;
  articleId: string;
};

export async function ArticleDetailPage({ className, articleId }: ArticleDetailPageProps) {
  const article = await fetchArticleDetail(articleId);

  return (
    <article className={cn("space-y-xl", className)}>
      <ArticleDetailContent article={article} />

      <div className="flex flex-wrap items-center gap-sm border-t border-hairline-soft pt-lg">
        <ErrorBoundary fallback={null}>
          <Suspense fallback={<ArticleLikeButtonSkeleton />}>
            <ArticleLikeButton articleId={articleId} />
          </Suspense>
        </ErrorBoundary>
        {article.sourceUrl && (
          <Button variant="link" asChild>
            <a href={article.sourceUrl} target="_blank" rel="noreferrer">
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
          <ArticleComments articleId={articleId} />
        </Suspense>
      </ErrorBoundary>
    </article>
  );
}

type ArticleDetailContentProps = {
  className?: string;
  article: ArticleDetailResponse;
};

async function ArticleDetailContent({ className, article }: ArticleDetailContentProps) {
  const locale = await getLocale();

  return (
    <div className={cn("space-y-lg", className)}>
      <header className="space-y-sm">
        <p className="text-caption text-meta">
          {article.source}
          {article.publishedAt && (
            <>
              {" · "}
              <time dateTime={article.publishedAt}>{formatDate(article.publishedAt, locale)}</time>
            </>
          )}
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

      <p className="text-body-lg whitespace-pre-wrap text-body">{article.summary}</p>
    </div>
  );
}
