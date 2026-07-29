import { listNewsSitemapArticles } from "@/shared/api";
import { SITE_NAME } from "@/shared/config";
import { buildNewsSitemap } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";
import { connection } from "next/server";

export async function GET() {
  await connection();

  const articles = await fetchRecentNewsArticles();
  const xml = buildNewsSitemap(articles, SITE_NAME);

  return new Response(xml, {
    headers: { "content-type": "application/xml; charset=utf-8" },
  });
}

async function fetchRecentNewsArticles() {
  "use cache";

  cacheLife("minutes");
  cacheTag("sitemap:news");

  try {
    const response = await listNewsSitemapArticles();
    return response.status === 200 ? response.data.items : [];
  } catch (error) {
    console.error("Failed to fetch articles for news sitemap", error);
    return [];
  }
}
