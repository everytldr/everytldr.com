import { ArticleScrollRow } from "@/entities/article";
import { SITE_URL } from "@/shared/config";
import { getPathname, type Locale } from "@/shared/i18n";
import {
  buildBriefingDetailUrl,
  buildBriefingJsonLd,
  cn,
  formatDateWithWeekday,
  markdownToPlainText,
  serializeJsonLd,
} from "@/shared/lib";
import { Container, MarkdownContent, Translation } from "@/shared/ui";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { fetchBriefing } from "../api/fetch-briefing";
import { BriefingDateNav } from "./briefing-date-nav";

type BriefingDetailPageProps = {
  className?: string;
  date: string;
  locale: Locale;
};

export async function BriefingDetailPage({ className, date, locale }: BriefingDetailPageProps) {
  const briefing = await fetchBriefing(date, locale);

  const jsonLd = buildBriefingJsonLd({
    url: `${SITE_URL}${getPathname({ locale, href: buildBriefingDetailUrl(date) })}`,
    headline: briefing.title,
    description: markdownToPlainText(briefing.content),
    datePublished: briefing.date,
  });

  return (
    <main className={cn("py-lg", className)}>
      <Container size="sm">
        <article className="space-y-xl">
          <script
            type="application/ld+json"
            dangerouslySetInnerHTML={{ __html: serializeJsonLd(jsonLd) }}
          />

          <div className="space-y-lg">
            <header className="space-y-sm">
              <p className="text-display-md text-ink">
                <Translation tKey="briefings.dateline-prefix" />{" "}
                <time dateTime={briefing.date}>{formatDateWithWeekday(briefing.date, locale)}</time>
              </p>
              <h1 className="text-display-xl text-ink">{briefing.title}</h1>
            </header>

            <MarkdownContent markdown={briefing.content} />
          </div>

          {briefing.articles.length > 0 && (
            <section className="space-y-sm border-t border-hairline-soft pt-lg">
              <Translation
                className="text-display-md text-ink"
                as="h2"
                tKey="briefings.sources-heading"
              />
              <ArticleScrollRow articles={briefing.articles} locale={locale} />
            </section>
          )}

          <ErrorBoundary fallback={null}>
            <Suspense fallback={null}>
              <BriefingDateNav
                className="border-t border-hairline-soft pt-lg"
                date={date}
                locale={locale}
              />
            </Suspense>
          </ErrorBoundary>
        </article>
      </Container>
    </main>
  );
}
