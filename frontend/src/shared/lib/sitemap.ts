import { SITE_URL } from "@/shared/config";
import { getPathname, isLocale, type Locale, locales } from "@/shared/i18n";
import type { MetadataRoute } from "next";
import { buildArticleDetailUrl } from "./url";

type SitemapEntry = MetadataRoute.Sitemap[number];

type EntryOptions = {
  changeFrequency?: SitemapEntry["changeFrequency"];
  priority?: number;
  lastModified?: string;
};

type SitemapArticle = {
  publishedAt: string;
  languages: string[];
  id: string;
};

function buildLocaleUrl(locale: Locale, path: string): string {
  return `${SITE_URL}${getPathname({ locale, href: path })}`;
}

export function buildLocalizedEntries(
  path: string,
  options: EntryOptions = {},
  availableLocales: Locale[] = locales,
): MetadataRoute.Sitemap {
  const languages = Object.fromEntries(
    availableLocales.map((locale) => [locale, buildLocaleUrl(locale, path)]),
  );

  return availableLocales.map((locale) => ({
    url: buildLocaleUrl(locale, path),
    lastModified: options.lastModified,
    changeFrequency: options.changeFrequency,
    priority: options.priority,
    alternates: { languages },
  }));
}

export function buildArticleEntries(article: SitemapArticle): MetadataRoute.Sitemap {
  const availableLocales = article.languages.filter(isLocale);
  if (availableLocales.length === 0) {
    return [];
  }

  return buildLocalizedEntries(
    buildArticleDetailUrl(article.id),
    { changeFrequency: "weekly", priority: 0.6, lastModified: article.publishedAt },
    availableLocales,
  );
}

export function buildUrlSet(entries: MetadataRoute.Sitemap): string {
  const urls = entries
    .map((entry) => {
      const alternates = Object.entries(entry.alternates?.languages ?? {}).map(
        ([hreflang, href]) =>
          `    <xhtml:link rel="alternate" hreflang="${escapeXml(hreflang)}" href="${escapeXml(String(href))}" />`,
      );

      return [
        "  <url>",
        `    <loc>${escapeXml(entry.url)}</loc>`,
        ...alternates,
        ...(entry.lastModified
          ? [`    <lastmod>${escapeXml(new Date(entry.lastModified).toISOString())}</lastmod>`]
          : []),
        "  </url>",
      ].join("\n");
    })
    .join("\n");

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">',
    urls,
    "</urlset>",
    "",
  ].join("\n");
}

export function buildSitemapIndex(sitemapUrls: string[]): string {
  const entries = sitemapUrls
    .map((url) => ["  <sitemap>", `    <loc>${escapeXml(url)}</loc>`, "  </sitemap>"].join("\n"))
    .join("\n");

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    entries,
    "</sitemapindex>",
    "",
  ].join("\n");
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}
