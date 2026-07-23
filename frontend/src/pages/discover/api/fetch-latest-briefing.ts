import "server-only";

import type { BriefingListItem } from "@/shared/api";
import { listBriefings } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import type { Nullable } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";

export async function fetchLatestBriefing(locale: Locale): Promise<Nullable<BriefingListItem>> {
  "use cache";

  cacheLife("hours");
  cacheTag(`briefings:${locale}`);

  const response = await listBriefings({ size: 1 }, { headers: { "Accept-Language": locale } });

  return response.status === 200 ? (response.data.items[0] ?? null) : null;
}
