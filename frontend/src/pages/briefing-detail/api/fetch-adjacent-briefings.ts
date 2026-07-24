import "server-only";

import { listBriefings, type BriefingListItem } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import type { Nullable } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";

const SCAN_SIZE = 50;

export type AdjacentBriefings = {
  previous: Nullable<BriefingListItem>;
  next: Nullable<BriefingListItem>;
};

export async function fetchAdjacentBriefings(
  date: string,
  locale: Locale,
): Promise<AdjacentBriefings> {
  "use cache";

  cacheLife("hours");
  cacheTag(`briefings:${locale}`);

  const response = await listBriefings(
    { size: SCAN_SIZE },
    {
      headers: {
        "Accept-Language": locale,
      },
    },
  );
  if (response.status !== 200) {
    return { previous: null, next: null };
  }

  const items = response.data.items;
  const index = items.findIndex((briefing) => briefing.date === date);
  if (index === -1) {
    return { previous: null, next: null };
  }

  return {
    previous: items[index + 1] ?? null,
    next: items[index - 1] ?? null,
  };
}
