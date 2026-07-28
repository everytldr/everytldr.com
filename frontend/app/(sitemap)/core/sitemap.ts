import { ROUTABLE_CATEGORY_NODES, STATIC_PAGE_URLS } from "@/shared/config";
import { buildCategoryUrl, buildLocalizedEntries } from "@/shared/lib";
import type { MetadataRoute } from "next";

const STATIC_PATHS = Object.values(STATIC_PAGE_URLS).filter((url) => url.startsWith("/"));

export default function sitemap(): MetadataRoute.Sitemap {
  const browsePaths = Array.from(new Set(["/", ...ROUTABLE_CATEGORY_NODES.map(buildCategoryUrl)]));

  const browseEntries = browsePaths.flatMap((path) =>
    buildLocalizedEntries(path, { changeFrequency: "daily", priority: path === "/" ? 1 : 0.8 }),
  );

  const staticEntries = STATIC_PATHS.flatMap((path) =>
    buildLocalizedEntries(path, { changeFrequency: "monthly", priority: 0.3 }),
  );

  return [...browseEntries, ...staticEntries];
}
