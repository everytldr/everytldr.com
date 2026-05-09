import { type CategoryNode, MainCategorySlug, SubCategorySlug } from "@/shared/config";

function buildCategoryUrl(main: MainCategorySlug, sub: SubCategorySlug): string {
  if (main === MainCategorySlug.Home && sub === SubCategorySlug.Discover) {
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
