import { ArticleScrollRow } from "@/entities/article";
import { SITE_URL } from "@/shared/config";
import { getPathname, type Locale } from "@/shared/i18n";
import {
  buildBriefingDetailUrl,
  buildBriefingJsonLd,
  cn,
  formatDateWithWeekday,
  serializeJsonLd,
  toMetaDescription,
} from "@/shared/lib";
import { Container, MarkdownContent, Translation } from "@/shared/ui";
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
    description: toMetaDescription(briefing.content),
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

            {briefing.requiresShareAlike && (
              <p className="text-caption text-meta">
                <a
                  className="underline underline-offset-4 outline-none hover:text-primary focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:text-primary-pressed"
                  href="https://creativecommons.org/licenses/by-sa/4.0/"
                  target="_blank"
                  rel="noreferrer license"
                >
                  <Translation tKey="briefings.share-alike-notice" />
                </a>
              </p>
            )}
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

          {(briefing.previousDate || briefing.nextDate) && (
            <BriefingDateNav
              className="border-t border-hairline-soft pt-lg"
              previousDate={briefing.previousDate}
              nextDate={briefing.nextDate}
              locale={locale}
            />
          )}
        </article>
      </Container>
    </main>
  );
}
