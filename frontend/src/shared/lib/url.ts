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
