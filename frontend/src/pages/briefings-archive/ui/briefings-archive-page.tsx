import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { range } from "lodash-es";
import { connection } from "next/server";
import { Suspense } from "react";
import { fetchBriefings } from "../api/fetch-briefings";
import { BriefingRow, BriefingRowSkeleton } from "./briefing-row";

const ARCHIVE_SIZE = 30;
const SKELETON_COUNT = 6;

type BriefingsArchivePageProps = {
  className?: string;
  locale: Locale;
};

export function BriefingsArchivePage({ className, locale }: BriefingsArchivePageProps) {
  return (
    <main className={cn("py-lg", className)}>
      <Container className="space-y-sm">
        <Translation className="text-display-lg text-ink" as="h1" tKey="briefings.title" />
        <Suspense
          fallback={
            <ul className="space-y-sm">
              {range(SKELETON_COUNT).map((i) => (
                <li key={i}>
                  <BriefingRowSkeleton />
                </li>
              ))}
            </ul>
          }
        >
          <BriefingsArchiveList locale={locale} />
        </Suspense>
      </Container>
    </main>
  );
}

type BriefingsArchiveListProps = {
  className?: string;
  locale: Locale;
};

async function BriefingsArchiveList({ className, locale }: BriefingsArchiveListProps) {
  await connection();

  const briefings = await fetchBriefings(locale, ARCHIVE_SIZE);

  return briefings.length === 0 ? (
    <Translation
      className="rounded-md border border-hairline-soft bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
      as="p"
      tKey="briefings.empty-state"
    />
  ) : (
    <ul className={cn("space-y-sm", className)}>
      {briefings.map((briefing) => (
        <li key={briefing.date}>
          <BriefingRow briefing={briefing} locale={locale} />
        </li>
      ))}
    </ul>
  );
}
