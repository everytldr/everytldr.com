"use client";

import {
  getListBriefingsInfiniteQueryKey,
  getListBriefingsSuspenseInfiniteQueryOptions,
  type BriefingListItem,
  type listBriefingsResponse,
} from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { cn, type Optional } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { useSuspenseInfiniteQuery } from "@tanstack/react-query";
import { range } from "lodash-es";
import { InView } from "react-intersection-observer";
import { BriefingRow, BriefingRowSkeleton } from "./briefing-row";

const NEXT_PAGE_SKELETON_COUNT = 3;

type BriefingListInfiniteProps = {
  className?: string;
  locale: Locale;
};

export function BriefingListInfinite({ className, locale }: BriefingListInfiniteProps) {
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = useSuspenseInfiniteQuery({
    ...getListBriefingsSuspenseInfiniteQueryOptions(undefined, {
      query: {
        queryKey: [...getListBriefingsInfiniteQueryKey(), locale],
      },
      request: {
        headers: {
          "Accept-Language": locale,
        },
      },
    }),
    getNextPageParam: (lastPage: listBriefingsResponse): Optional<string> =>
      lastPage.status === 200 ? (lastPage.data.nextCursor ?? undefined) : undefined,
  });

  const briefings: BriefingListItem[] = data.pages.flatMap((page) =>
    page.status === 200 ? page.data.items : [],
  );

  return briefings.length === 0 ? (
    <Translation
      className="rounded-md border border-hairline-soft bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
      as="p"
      tKey="briefings.empty-state"
    />
  ) : (
    <div className={className}>
      <ul className="divide-y divide-hairline-soft border-t border-hairline-soft">
        {briefings.map((briefing) => (
          <li key={briefing.date}>
            <BriefingRow briefing={briefing} locale={locale} />
          </li>
        ))}
        {isFetchingNextPage &&
          range(NEXT_PAGE_SKELETON_COUNT).map((i) => (
            <li key={i}>
              <BriefingRowSkeleton />
            </li>
          ))}
      </ul>
      {hasNextPage && (
        <InView
          key={data.pages.length}
          as="div"
          rootMargin="200px"
          aria-hidden="true"
          onChange={handleSentinelChange}
        />
      )}
    </div>
  );

  function handleSentinelChange(inView: boolean) {
    if (inView && !isFetchingNextPage) {
      fetchNextPage();
    }
  }
}

type BriefingListSkeletonProps = {
  className?: string;
  count: number;
};

export function BriefingListSkeleton({ className, count }: BriefingListSkeletonProps) {
  return (
    <ul
      className={cn("divide-y divide-hairline-soft border-t border-hairline-soft", className)}
      aria-hidden="true"
    >
      {range(count).map((i) => (
        <li key={i}>
          <BriefingRowSkeleton />
        </li>
      ))}
    </ul>
  );
}
