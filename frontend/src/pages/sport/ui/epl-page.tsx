import { EplTabSlug, type EplTeam } from "@/shared/config";
import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container } from "@/shared/ui";
import { EplNewsTab } from "./epl-news-tab";
import { EplStandingsSection } from "./epl-standings-section";
import { EplTabs } from "./epl-tabs";

type EplPageProps = {
  className?: string;
  locale: Locale;
  subSlug?: EplTabSlug | EplTeam;
};

type ResolvedSubSlug =
  | { activeTab: EplTabSlug.News; filter?: EplTeam }
  | { activeTab: EplTabSlug.Record };

export function EplPage({ className, locale, subSlug }: EplPageProps) {
  const resolved = resolveSubSlug(subSlug);

  return (
    <main className={cn("py-lg", className)}>
      <Container className="space-y-sm">
        <EplTabs activeTab={resolved.activeTab} />

        {resolved.activeTab === EplTabSlug.News ? (
          <EplNewsTab filter={resolved.filter} locale={locale} />
        ) : resolved.activeTab === EplTabSlug.Record ? (
          <EplStandingsSection />
        ) : null}
      </Container>
    </main>
  );
}

function resolveSubSlug(subSlug?: EplTabSlug | EplTeam): ResolvedSubSlug {
  if (subSlug === EplTabSlug.Record) {
    return { activeTab: EplTabSlug.Record };
  }
  if (subSlug === EplTabSlug.News) {
    return { activeTab: EplTabSlug.News };
  }

  return { activeTab: EplTabSlug.News, filter: subSlug };
}
