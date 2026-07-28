import { listSitemapArticles } from "@/shared/api";
import { SITE_URL, SITEMAP_ARTICLE_CHUNK_SIZE } from "@/shared/config";
import { buildSitemapIndex } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";
import { connection } from "next/server";

export async function GET() {
  await connection();

  const total = await fetchArticleTotal();
  const chunkCount = countSitemapChunks(total, SITEMAP_ARTICLE_CHUNK_SIZE);

  const articleSitemapUrls = Array.from(
    { length: chunkCount },
    (_, id) => `${SITE_URL}/articles/sitemap/${id}.xml`,
  );

  const xml = buildSitemapIndex([
    `${SITE_URL}/core/sitemap.xml`,
    `${SITE_URL}/news-sitemap.xml`,
    ...articleSitemapUrls,
  ]);

  return new Response(xml, {
    headers: { "content-type": "application/xml; charset=utf-8" },
  });
}

async function fetchArticleTotal(): Promise<number> {
  "use cache";

  cacheLife("hours");
  cacheTag("sitemap:articles");

  try {
    const response = await listSitemapArticles({ page: 0, size: 1 });
    return response.status === 200 ? response.data.total : 0;
  } catch (error) {
    console.error("Failed to fetch article total for sitemap index", error);
    return 0;
  }
}

function countSitemapChunks(total: number, chunkSize: number): number {
  if (total === 0) {
    return 0;
  }

  return Math.max(1, Math.ceil(total / chunkSize));
}
