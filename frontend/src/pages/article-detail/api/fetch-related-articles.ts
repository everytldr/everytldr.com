import "server-only";

import { listRelatedArticles, type ArticleListItem } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { A_MINUTE, A_SECOND } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";

export async function fetchRelatedArticles(
  articleId: string,
  locale: Locale,
  size: number,
): Promise<ArticleListItem[]> {
  "use cache";

  cacheLife({ revalidate: (30 * A_MINUTE) / A_SECOND });
  cacheTag(`article-related:${locale}:${articleId}`);

  const response = await listRelatedArticles(
    articleId,
    { size },
    {
      headers: {
        "Accept-Language": locale,
      },
    },
  );

  if (response.status !== 200) {
    throw new Error(
      `Unexpected response status ${response.status} for related articles of ${articleId}`,
    );
  }

  return response.data.items;
}
