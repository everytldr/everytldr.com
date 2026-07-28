import { listSitemapArticles } from "@/shared/api";
import { SITEMAP_ARTICLE_CHUNK_SIZE } from "@/shared/config";
import { buildArticleEntries, buildUrlSet, type Nullable } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";

type RouteContext = { params: Promise<{ chunk: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  const { chunk } = await params;
  const page = parseChunkPage(chunk);
  if (page === null) {
    return new Response(null, { status: 404 });
  }

  const articles = await fetchArticleChunk(page);
  if (articles.length === 0) {
    return new Response(null, { status: 404 });
  }

  const xml = buildUrlSet(articles.flatMap(buildArticleEntries));

  return new Response(xml, {
    headers: { "content-type": "application/xml; charset=utf-8" },
  });
}

function parseChunkPage(chunk: string): Nullable<number> {
  const match = /^(\d+)\.xml$/.exec(chunk);
  if (!match) {
    return null;
  }

  return Number(match[1]);
}

async function fetchArticleChunk(page: number) {
  "use cache";

  cacheLife("hours");
  cacheTag(`sitemap:articles:${page}`);

  try {
    const response = await listSitemapArticles({ page, size: SITEMAP_ARTICLE_CHUNK_SIZE });
    return response.status === 200 ? response.data.items : [];
  } catch (error) {
    console.error("Failed to fetch articles for sitemap", error);
    return [];
  }
}
