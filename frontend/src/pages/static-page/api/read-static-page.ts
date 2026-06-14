import { type Locale } from "@/shared/i18n";
import { markdownToPlainText } from "@/shared/lib";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import "server-only";

export const staticPageSlugs = ["about", "privacy", "terms"] as const;

export type StaticPageSlug = (typeof staticPageSlugs)[number];

export type StaticPageContent = {
  description: string;
  markdown: string;
  title: string;
};

export function readStaticPage(slug: StaticPageSlug, locale: Locale): StaticPageContent {
  const filePath = join(process.cwd(), "docs", "pages", `${slug}-${locale}.md`);
  const source = readFileSync(filePath, "utf8");

  return parseStaticPage(source);
}

function parseStaticPage(source: string): StaticPageContent {
  const normalized = source.trim();
  const titleMatch = /^#\s+(.+)$/m.exec(normalized);
  const title = titleMatch?.[1]?.trim() ?? "everytldr";
  const markdown = normalized.replace(/^#\s+.+\n?/, "").trim();
  const description = truncateDescription(markdownToPlainText(markdown));

  return { description, markdown, title };
}

function truncateDescription(value: string): string {
  const maxLength = 160;
  if (value.length <= maxLength) {
    return value;
  }

  const truncated = value.slice(0, maxLength).trimEnd();
  return `${truncated}...`;
}
