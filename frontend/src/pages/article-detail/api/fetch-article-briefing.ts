import "server-only";

import { getArticleBriefing, type ArticleBriefingResponse } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { A_MINUTE, A_SECOND, type Nullable } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";

export async function fetchArticleBriefing(
  articleId: string,
  locale: Locale,
): Promise<Nullable<ArticleBriefingResponse>> {
  "use cache";

  cacheLife({ revalidate: (30 * A_MINUTE) / A_SECOND });
  cacheTag(`article-briefing:${locale}:${articleId}`);

  const response = await getArticleBriefing(articleId, {
    headers: { "Accept-Language": locale },
  });

  if (response.status === 404) {
    return null;
  }

  if (response.status !== 200) {
    throw new Error(
      `Unexpected response status ${response.status} for briefing of article ${articleId}`,
    );
  }

  return response.data;
}
