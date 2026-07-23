import "server-only";

import { listBriefings, type BriefingListItem } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { cacheLife, cacheTag } from "next/cache";

export async function fetchBriefings(locale: Locale, size: number): Promise<BriefingListItem[]> {
  "use cache";

  cacheLife("hours");
  cacheTag(`briefings:${locale}`);

  const response = await listBriefings(
    { size },
    {
      headers: {
        "Accept-Language": locale,
      },
    },
  );
  return response.status === 200 ? response.data.items : [];
}
