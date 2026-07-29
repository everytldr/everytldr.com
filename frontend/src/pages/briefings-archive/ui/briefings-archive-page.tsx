import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { HydrationBoundary } from "@tanstack/react-query";
import { connection } from "next/server";
import { Suspense } from "react";
import { prefetchBriefings } from "../api/prefetch-briefings";
import { BriefingListInfinite, BriefingListSkeleton } from "./briefing-list-infinite";

const SKELETON_COUNT = 6;

type BriefingsArchivePageProps = {
  className?: string;
  locale: Locale;
};

export function BriefingsArchivePage({ className, locale }: BriefingsArchivePageProps) {
  return (
    <main className={cn("py-lg", className)}>
      <Container className="space-y-lg">
        <header className="space-y-xs">
          <Translation className="text-display-lg text-ink" as="h1" tKey="briefings.title" />
          <Translation className="text-body-md text-meta" as="p" tKey="briefings.description" />
        </header>
        <Suspense fallback={<BriefingListSkeleton count={SKELETON_COUNT} />}>
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

  const dehydratedState = await prefetchBriefings(locale);

  return (
    <HydrationBoundary state={dehydratedState}>
      <BriefingListInfinite className={className} locale={locale} />
    </HydrationBoundary>
  );
}
