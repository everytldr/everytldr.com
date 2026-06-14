import { SITE_URL } from "@/shared/config";
import { getPathname, type Locale } from "@/shared/i18n";
import { markdownToPlainText } from "./markdown";
import { buildArticleDetailUrl } from "./url";

const EXCERPT_LENGTH = 200;

type RssItem = {
  title: string;
  link: string;
  guid: string;
  pubDate: string;
  description: string;
};

type BuildRssFeedParams = {
  title: string;
  description: string;
  link: string;
  feedUrl: string;
  language: string;
  items: RssItem[];
};

type ArticleFeedSource = {
  title: string;
  summary: string;
  publishedAt: string;
  id: string;
};

export function buildArticleFeedItem(article: ArticleFeedSource, locale: Locale): RssItem {
  const link = `${SITE_URL}${getPathname({ locale, href: buildArticleDetailUrl(article.id) })}`;
  const text = markdownToPlainText(article.summary);

  return {
    title: article.title,
    link,
    guid: link,
    pubDate: new Date(article.publishedAt).toUTCString(),
    description:
      text.length > EXCERPT_LENGTH ? `${text.slice(0, EXCERPT_LENGTH).trimEnd()}…` : text,
  };
}

export function buildRssFeed({
  title,
  description,
  link,
  feedUrl,
  language,
  items,
}: BuildRssFeedParams): string {
  const entries = items
    .map((item) =>
      [
        "    <item>",
        `      <title>${escapeXml(item.title)}</title>`,
        `      <link>${escapeXml(item.link)}</link>`,
        `      <guid isPermaLink="true">${escapeXml(item.guid)}</guid>`,
        `      <pubDate>${item.pubDate}</pubDate>`,
        `      <description>${escapeXml(item.description)}</description>`,
        "    </item>",
      ].join("\n"),
    )
    .join("\n");

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">',
    "  <channel>",
    `    <title>${escapeXml(title)}</title>`,
    `    <link>${escapeXml(link)}</link>`,
    `    <description>${escapeXml(description)}</description>`,
    `    <language>${escapeXml(language)}</language>`,
    `    <atom:link href="${escapeXml(feedUrl)}" rel="self" type="application/rss+xml" />`,
    entries,
    "  </channel>",
    "</rss>",
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
