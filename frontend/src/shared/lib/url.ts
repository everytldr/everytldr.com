import {
  type CategoryNode,
  DEFAULT_CATEGORY_NODE,
  EplTabSlug,
  type EplTeam,
} from "@/shared/config";

export function buildCategoryUrl(node: CategoryNode): string {
  if (node.redirectPath) {
    return node.redirectPath;
  }
  if (node.slug === DEFAULT_CATEGORY_NODE.slug) {
    return "/";
  }

  return `/${node.slug}`;
}

export function buildEplFilterUrl(team?: EplTeam): string {
  return team ? `/epl/${team}` : "/epl";
}

export function buildEplTabUrl(tab: EplTabSlug): string {
  return tab === EplTabSlug.News ? "/epl" : `/epl/${tab}`;
}

export function buildSearchUrl(query: string): string {
  const trimmed = query.trim();
  if (trimmed.length === 0) {
    return "/search";
  }
  return `/search?q=${encodeURIComponent(trimmed)}`;
}

export function buildArticleDetailUrl(articleId: string): string {
  return `/articles/${articleId}`;
}

export function buildBriefingArchiveUrl(): string {
  return "/briefings";
}

export function buildBriefingDetailUrl(briefingDate: string): string {
  return `/briefings/${briefingDate}`;
}

const WSRV_ENDPOINT = "https://wsrv.nl/";

export function buildOgImageUrl(src: string): string {
  if (!isWebpUrl(src)) {
    return src;
  }
  return `${WSRV_ENDPOINT}?url=${encodeURIComponent(src)}&output=jpg`;
}

function isWebpUrl(src: string): boolean {
  try {
    return new URL(src).pathname.toLowerCase().endsWith(".webp");
  } catch {
    return src.toLowerCase().endsWith(".webp");
  }
}
