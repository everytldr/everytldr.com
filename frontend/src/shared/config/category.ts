import { assert, ensure } from "@/shared/lib";

export enum EplTeam {
  Arsenal = "arsenal",
}

export enum MainCategorySlug {
  Home = "home",
  Sport = "sport",
  Politics = "politics",
  Technology = "technology",
}

export enum SubCategorySlug {
  Discover = "discover",
  Latest = "latest",
  Trending = "trending",
  EPL = "epl",
  Domestic = "domestic",
  Ai = "ai",
  NBA = "nba",
}

export type CategoryNode = {
  slug: MainCategorySlug;
  subs: SubCategorySlug[];
};

export const CATEGORIES: CategoryNode[] = [
  {
    slug: MainCategorySlug.Home,
    subs: [SubCategorySlug.Discover, SubCategorySlug.Latest, SubCategorySlug.Trending],
  },
  { slug: MainCategorySlug.Sport, subs: [SubCategorySlug.EPL, SubCategorySlug.NBA] },
  { slug: MainCategorySlug.Politics, subs: [SubCategorySlug.Domestic] },
  { slug: MainCategorySlug.Technology, subs: [SubCategorySlug.Ai] },
];

export const HOME_CATEGORY_NODE: CategoryNode = ensure(
  CATEGORIES.find((c) => c.slug === MainCategorySlug.Home),
);

export const SUB_CATEGORY_SLUGS: SubCategorySlug[] = CATEGORIES.flatMap((c) => c.subs);

export const DEFAULT_SUB_CATEGORY_SLUG: SubCategorySlug = SubCategorySlug.Discover;

export function findRootCategory(sub: SubCategorySlug): CategoryNode {
  const category = CATEGORIES.find((c) => c.subs.includes(sub));
  assert(category, "Invalid subcategory slug");
  return category;
}
