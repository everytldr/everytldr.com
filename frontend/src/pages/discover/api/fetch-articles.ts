import "server-only";

import type { ArticleListItem } from "@/shared/api";
import { listArticles } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import type { Optional } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";

export async function fetchArticles(
  categoryPrefix: Optional<string>,
  locale: Locale,
  size: number,
): Promise<ArticleListItem[]> {
  "use cache";

  cacheLife("minutes");
  cacheTag(`articles:${locale}:${categoryPrefix ?? "latest"}`);

  const response = await listArticles(
    { categoryPrefix, size },
    { headers: { "Accept-Language": locale } },
  );

  return response.status === 200 ? (response.data.items ?? []) : [];
}
