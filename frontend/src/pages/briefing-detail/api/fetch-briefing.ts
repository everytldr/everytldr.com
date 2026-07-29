import "server-only";

import { ApiError, getBriefing, type BriefingDetailResponse } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { cacheLife, cacheTag } from "next/cache";
import { notFound } from "next/navigation";

export async function fetchBriefing(date: string, locale: Locale): Promise<BriefingDetailResponse> {
  "use cache";

  cacheLife("hours");
  cacheTag(`briefing:${locale}:${date}`);

  try {
    const response = await getBriefing(date, {
      headers: {
        "Accept-Language": locale,
      },
    });
    if (response.status === 200) {
      return response.data;
    }
    throw new Error(`Unexpected response status ${response.status} for briefing ${date}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
