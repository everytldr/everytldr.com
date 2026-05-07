import { assert } from "@/shared/lib";

export enum MainCategorySlug {
  Home = "home",
  Sport = "sport",
  Politics = "politics",
  Technology = "technology",
}

export enum SubCategorySlug {
  Latest = "latest",
  Trending = "trending",
  Epl = "epl",
  Domestic = "domestic",
  Ai = "ai",
}

export type CategoryNode = {
  slug: MainCategorySlug;
  subs: SubCategorySlug[];
};

export const CATEGORIES: CategoryNode[] = [
  {
    slug: MainCategorySlug.Home,
    subs: [SubCategorySlug.Latest, SubCategorySlug.Trending],
  },
  { slug: MainCategorySlug.Sport, subs: [SubCategorySlug.Epl] },
  { slug: MainCategorySlug.Politics, subs: [SubCategorySlug.Domestic] },
  { slug: MainCategorySlug.Technology, subs: [SubCategorySlug.Ai] },
];

export const SUB_CATEGORY_SLUGS: SubCategorySlug[] = CATEGORIES.flatMap((c) => c.subs);

export function findRootCategory(sub: SubCategorySlug): CategoryNode {
  const category = CATEGORIES.find((c) => c.subs.includes(sub));
  assert(category, "Invalid subcategory slug");
  return category;
}
