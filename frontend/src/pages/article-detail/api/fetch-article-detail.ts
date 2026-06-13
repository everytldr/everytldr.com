import "server-only";

import { ApiError, getArticle, type ArticleDetailResponse } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { A_MINUTE, A_SECOND } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";
import { notFound } from "next/navigation";

export async function fetchArticleDetail(
  articleId: string,
  locale: Locale,
): Promise<ArticleDetailResponse> {
  "use cache";

  cacheLife({ revalidate: (15 * A_MINUTE) / A_SECOND });
  cacheTag(`article:${locale}:${articleId}`);

  try {
    const response = await getArticle(articleId, {
      headers: {
        "Accept-Language": locale,
      },
    });
    if (response.status === 200) {
      return response.data;
    }
    throw new Error(`Unexpected response status ${response.status} for article ${articleId}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
