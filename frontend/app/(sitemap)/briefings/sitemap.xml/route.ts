import { listSitemapBriefings } from "@/shared/api";
import { SITEMAP_BRIEFING_PAGE_SIZE } from "@/shared/config";
import { buildBriefingEntries, buildUrlSet } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";
import { connection } from "next/server";

export async function GET() {
  await connection();

  const briefings = await fetchSitemapBriefings();
  const xml = buildUrlSet(briefings.flatMap(buildBriefingEntries));

  return new Response(xml, {
    headers: { "content-type": "application/xml; charset=utf-8" },
  });
}

async function fetchSitemapBriefings() {
  "use cache";

  cacheLife("hours");
  cacheTag("sitemap:briefings");

  try {
    const response = await listSitemapBriefings({ page: 0, size: SITEMAP_BRIEFING_PAGE_SIZE });
    return response.status === 200 ? response.data.items : [];
  } catch (error) {
    console.error("Failed to fetch briefings for sitemap", error);
    return [];
  }
}
