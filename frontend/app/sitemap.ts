import { type ArticleListItem, listArticles } from "@/shared/api";
import { ROUTABLE_CATEGORY_NODES, SITE_URL, STATIC_PAGE_URLS } from "@/shared/config";
import { getPathname, type Locale, locales } from "@/shared/i18n";
import { buildArticleDetailUrl, buildCategoryUrl } from "@/shared/lib";
import type { MetadataRoute } from "next";
import { cacheLife, cacheTag } from "next/cache";

const STATIC_PATHS = Object.values(STATIC_PAGE_URLS).filter((url) => url.startsWith("/"));

const RECENT_ARTICLE_COUNT = 50;

type SitemapEntry = MetadataRoute.Sitemap[number];

type EntryOptions = {
  changeFrequency: SitemapEntry["changeFrequency"];
  priority: number;
  lastModified?: string;
};

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const browsePaths = Array.from(new Set(["/", ...ROUTABLE_CATEGORY_NODES.map(buildCategoryUrl)]));

  const browseEntries = browsePaths.flatMap((path) =>
    buildLocalizedEntries(path, { changeFrequency: "daily", priority: path === "/" ? 1 : 0.8 }),
  );

  const staticEntries = STATIC_PATHS.flatMap((path) =>
    buildLocalizedEntries(path, { changeFrequency: "monthly", priority: 0.3 }),
  );

  const articles = await fetchRecentArticles();
  const articleEntries = articles.flatMap((article) =>
    buildLocalizedEntries(buildArticleDetailUrl(article.id), {
      changeFrequency: "weekly",
      priority: 0.6,
      lastModified: article.publishedAt,
    }),
  );

  return [...browseEntries, ...staticEntries, ...articleEntries];
}

async function fetchRecentArticles(): Promise<ArticleListItem[]> {
  "use cache";

  cacheLife("hours");
  cacheTag("sitemap:articles");

  try {
    const response = await listArticles({ size: RECENT_ARTICLE_COUNT });
    return response.status === 200 ? response.data.items : [];
  } catch (e) {
    console.error("Failed to fetch articles for sitemap", e);
    return [];
  }
}

function buildLocalizedEntries(path: string, options: EntryOptions): MetadataRoute.Sitemap {
  function buildLocaleUrl(locale: Locale, path: string): string {
    return `${SITE_URL}${getPathname({ locale, href: path })}`;
  }

  const languages = Object.fromEntries(
    locales.map((locale) => [locale, buildLocaleUrl(locale, path)]),
  );

  return locales.map((locale) => ({
    url: buildLocaleUrl(locale, path),
    lastModified: options.lastModified,
    changeFrequency: options.changeFrequency,
    priority: options.priority,
    alternates: { languages },
  }));
}
