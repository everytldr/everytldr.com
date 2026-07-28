import { type ArticleDetailResponse } from "@/shared/api";
import {
  ADSENSE_SLOT_ARTICLE_DETAIL,
  HIDE_ADSENSE,
  SITE_URL,
  STATIC_PAGE_URLS,
} from "@/shared/config";
import { getPathname, type Locale } from "@/shared/i18n";
import {
  buildArticleDetailUrl,
  buildNewsArticleJsonLd,
  cn,
  formatDate,
  markdownToPlainText,
  type Nullable,
  serializeJsonLd,
} from "@/shared/lib";
import { AdSlot, Button, ConditionalLink, MarkdownContent, Translation } from "@/shared/ui";
import { ExternalLink } from "lucide-react";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { fetchArticleDetail } from "../api/fetch-article-detail";
import { ArticleCategoryPath } from "./article-category-path";
import { ArticleComments, ArticleCommentsError, ArticleCommentsSkeleton } from "./article-comments";
import { ArticleLikeButton } from "./article-like-button";
import { ArticleShareButton } from "./article-share-button";
import { ArticleViewTracker } from "./article-view-tracker";
import { RelatedArticles, RelatedArticlesSkeleton } from "./related-articles";

type ArticleDetailPageProps = {
  className?: string;
  articleId: string;
  locale: Locale;
};

export async function ArticleDetailPage({ className, articleId, locale }: ArticleDetailPageProps) {
  const article = await fetchArticleDetail(articleId, locale);
  const articleUrl = `${SITE_URL}${getPathname({ locale, href: buildArticleDetailUrl(articleId) })}`;

  const jsonLd = buildNewsArticleJsonLd({
    url: articleUrl,
    headline: article.title,
    description: markdownToPlainText(article.summary),
    datePublished: article.publishedAt,
    isBasedOn: article.contentUrl,
  });

  return (
    <article className={cn("space-y-xl", className)}>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: serializeJsonLd(jsonLd) }}
      />
      <ArticleViewTracker articleId={articleId} />

      <ArticleDetailContent article={article} articleUrl={articleUrl} locale={locale} />

      <div className="flex flex-wrap items-center gap-sm">
        <ArticleLikeButton articleId={articleId} />
        <ArticleShareButton variant="labeled" url={articleUrl} title={article.title} />
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

      <ErrorBoundary fallback={null}>
        <Suspense fallback={<RelatedArticlesSkeleton />}>
          <RelatedArticles
            className="border-t border-hairline-soft pt-lg"
            articleId={articleId}
            locale={locale}
          />
        </Suspense>
      </ErrorBoundary>

      {!HIDE_ADSENSE && article.advertisingAllowed && (
        <AdSlot className="w-full" slot={ADSENSE_SLOT_ARTICLE_DETAIL} />
      )}

      <ErrorBoundary fallback={<ArticleCommentsError />}>
        <Suspense fallback={<ArticleCommentsSkeleton />}>
          <ArticleComments
            className="border-t border-hairline-soft pt-lg"
            articleId={articleId}
            locale={locale}
          />
        </Suspense>
      </ErrorBoundary>
    </article>
  );
}

type ArticleDetailContentProps = {
  className?: string;
  article: ArticleDetailResponse;
  articleUrl: string;
  locale: Locale;
};

function ArticleDetailContent({
  className,
  article,
  articleUrl,
  locale,
}: ArticleDetailContentProps) {
  return (
    <div className={cn("space-y-lg", className)}>
      <header className="space-y-sm">
        <ArticleCategoryPath category={article.category} />
        <h1 className="text-display-xl text-ink">{article.title}</h1>
        <div className="flex items-center justify-between gap-sm">
          <div className="flex min-w-0 flex-col gap-2xs sm:flex-row sm:items-baseline sm:gap-2xs">
            <span className="truncate text-title-md text-ink">{article.source}</span>
            <p className="text-caption text-meta before:content-none sm:before:mr-2xs sm:before:content-['·'] [&>*:not(:last-child)]:after:mx-2xs [&>*:not(:last-child)]:after:content-['·']">
              <time dateTime={article.publishedAt}>{formatDate(article.publishedAt, locale)}</time>
              <Translation
                as="span"
                tKey="article-detail.view-count"
                values={{ count: article.viewCount }}
              />
            </p>
          </div>
          <ArticleShareButton variant="icon" url={articleUrl} title={article.title} />
        </div>
      </header>

      <MarkdownContent markdown={article.summary} />

      <p className="text-caption text-meta">
        <Translation tKey="article-detail.ai-disclosure" />{" "}
        <ConditionalLink
          className="underline underline-offset-4 outline-none hover:text-primary focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:text-primary-pressed"
          href={STATIC_PAGE_URLS.about}
        >
          <Translation tKey="article-detail.ai-disclosure-link" />
        </ConditionalLink>
        {article.requiresAttribution && (
          <>
            {" · "}
            <a
              className="underline underline-offset-4 outline-none hover:text-primary focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:text-primary-pressed"
              href={buildLicenseUrl(article.licenseCode, article.licenseVersion)}
              target="_blank"
              rel="noreferrer license"
            >
              {formatLicenseLabel(article.licenseCode, article.licenseVersion)}
            </a>
          </>
        )}
      </p>

      {article.requiresShareAlike && (
        <p className="text-caption text-meta">
          <a
            className="underline underline-offset-4 outline-none hover:text-primary focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:text-primary-pressed"
            href={buildLicenseUrl(article.licenseCode, article.licenseVersion)}
            target="_blank"
            rel="noreferrer license"
          >
            <Translation
              tKey="article-detail.share-alike-notice"
              values={{ license: formatLicenseLabel(article.licenseCode, article.licenseVersion) }}
            />
          </a>
        </p>
      )}
    </div>
  );
}

function formatLicenseLabel(licenseCode: string, licenseVersion: Nullable<string>) {
  const name = licenseCode.replace(/-/g, " ");
  return licenseVersion ? `${name} ${licenseVersion}` : name;
}

function buildLicenseUrl(licenseCode: string, licenseVersion: Nullable<string>) {
  const slug = licenseCode.replace(/^CC-/, "").toLowerCase();
  return `https://creativecommons.org/licenses/${slug}/${licenseVersion ?? "4.0"}/`;
}
