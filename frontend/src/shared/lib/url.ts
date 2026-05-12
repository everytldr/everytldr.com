import {
  type CategoryNode,
  DEFAULT_SUB_CATEGORY_SLUG,
  EplPageTab,
  type EplTeam,
  MainCategorySlug,
  SubCategorySlug,
} from "@/shared/config";

function buildCategoryUrl(main: MainCategorySlug, sub: SubCategorySlug): string {
  if (main === MainCategorySlug.Home && sub === DEFAULT_SUB_CATEGORY_SLUG) {
    return "/";
  }
  return `/${sub}`;
}

export function buildMainCategoryUrl(node: CategoryNode): string {
  const firstSub = node.subs?.[0];
  if (firstSub) {
    return buildCategoryUrl(node.slug, firstSub);
  }
  return "/";
}

export function buildSubcategoryUrl(parent: CategoryNode, sub: SubCategorySlug): string {
  return buildCategoryUrl(parent.slug, sub);
}

export function buildEplFilterUrl(team?: EplTeam): string {
  if (!team) {
    return `/${SubCategorySlug.EPL}`;
  }
  return `/${SubCategorySlug.EPL}/${team}`;
}

export function buildEplTabUrl(tab: EplPageTab): string {
  if (tab === EplPageTab.News) {
    return `/${SubCategorySlug.EPL}`;
  }
  return `/${SubCategorySlug.EPL}/${tab}`;
}
