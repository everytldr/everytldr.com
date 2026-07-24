import "server-only";

import {
  getListBriefingsInfiniteQueryKey,
  getListBriefingsSuspenseInfiniteQueryOptions,
  getQueryClient,
} from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { dehydrate } from "@tanstack/react-query";
import { cacheLife, cacheTag } from "next/cache";

export async function prefetchBriefings(locale: Locale) {
  "use cache";

  cacheLife("minutes");
  cacheTag(`briefings:${locale}`);

  const queryClient = getQueryClient();
  const queryOptions = getListBriefingsSuspenseInfiniteQueryOptions(undefined, {
    query: {
      queryKey: [...getListBriefingsInfiniteQueryKey(), locale],
    },
    request: {
      headers: {
        "Accept-Language": locale,
      },
    },
  });
  await queryClient.prefetchInfiniteQuery(queryOptions);

  return dehydrate(queryClient);
}
