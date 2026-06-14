import { assert, ensure } from "@/shared/lib";

export type CategoryNode = {
  slug: string;
  routable?: boolean;
  redirectPath?: `/${string}`;
  children?: readonly CategoryNode[];
};

export const BLOCKED_CATEGORY_SLUGS = (process.env.NEXT_PUBLIC_BLOCKED_CATEGORY_SLUGS ?? "")
  .split(",")
  .map((slug) => slug.trim())
  .filter(Boolean);

const BLOCKED_SLUG_SET = new Set(BLOCKED_CATEGORY_SLUGS);

export const CATEGORY_GRAPH = processGraph([
  {
    slug: "home",
    routable: false,
    redirectPath: "/",
    children: [{ slug: "discover" }, { slug: "trending" }, { slug: "latest" }],
  },
  {
    slug: "politics",
    children: [
      { slug: "politics-elections" },
      { slug: "politics-government" },
      { slug: "politics-law" },
    ],
  },
  {
    slug: "economy",
    children: [
      { slug: "economy-business" },
      { slug: "economy-consumer" },
      { slug: "economy-finance" },
      { slug: "economy-policy" },
      { slug: "economy-work" },
    ],
  },
  {
    slug: "society",
    children: [
      { slug: "society-activism" },
      { slug: "society-demographics" },
      { slug: "society-education" },
      { slug: "society-media" },
      { slug: "society-migration" },
      { slug: "society-rights" },
      { slug: "society-safety" },
    ],
  },
  {
    slug: "world",
    children: [
      { slug: "world-conflict" },
      { slug: "world-geopolitics" },
      { slug: "world-humanitarian" },
    ],
  },
  {
    slug: "technology",
    children: [
      { slug: "technology-ai" },
      { slug: "technology-cybersecurity" },
      { slug: "technology-devices" },
      { slug: "technology-games" },
      { slug: "technology-internet_platforms" },
      { slug: "technology-science" },
    ],
  },
  {
    slug: "culture",
    children: [
      { slug: "culture-arts" },
      { slug: "culture-history" },
      { slug: "culture-language" },
      { slug: "culture-lifestyle" },
      { slug: "culture-religion" },
    ],
  },
  {
    slug: "sport",
    children: [{ slug: "sport-events" }, { slug: "epl" }, { slug: "nba", routable: false }],
  },
  {
    slug: "health",
    children: [
      { slug: "health-healthcare" },
      { slug: "health-mental_health" },
      { slug: "health-public_health" },
      { slug: "health-wellness" },
    ],
  },
  {
    slug: "environment",
    children: [
      { slug: "environment-climate" },
      { slug: "environment-energy" },
      { slug: "environment-nature" },
      { slug: "environment-pollution" },
    ],
  },
] as const satisfies CategoryNode[]);

export const CATEGORY_NODES = CATEGORY_GRAPH.flatMap((node) => [node, ...(node.children ?? [])]);
export const ROUTABLE_CATEGORY_NODES = CATEGORY_NODES.filter(
  (node) => !("routable" in node) || node.routable,
);
export const LEAF_CATEGORY_SLUGS = CATEGORY_GRAPH.flatMap(
  (node) => node.children?.map((node) => node.slug) ?? [],
);
export const STATIC_CATEGORY_SLUGS = CATEGORY_GRAPH.flatMap((node) =>
  node.redirectPath ? (node.children?.map((child) => child.slug) ?? []) : [node.slug],
);
export const HOME_CATEGORY_NODE = ensure(CATEGORY_NODES.find((node) => node.slug === "home"));
export const DEFAULT_CATEGORY_NODE = ensure(
  CATEGORY_NODES.find((node) => node.slug === "discover"),
);

export type MainCategorySlug = (typeof CATEGORY_GRAPH)[number]["slug"];
export type LeafCategorySlug = (typeof LEAF_CATEGORY_SLUGS)[number];
export type CategorySlug = MainCategorySlug | LeafCategorySlug;

export function findRootCategory(slug: string) {
  function findRecursively(node: CategoryNode) {
    if (slug === node.slug) {
      return true;
    }
    return node.children?.some(findRecursively) || false;
  }

  const category = CATEGORY_GRAPH.find(findRecursively);
  assert(category, "Invalid subcategory slug");
  return category;
}

export function isMainCategorySlug(slug: CategorySlug): slug is MainCategorySlug {
  return CATEGORY_GRAPH.some((node) => node.slug === slug);
}

export function isFeedableCategory(slug: string): slug is CategorySlug {
  const isRoutable = ROUTABLE_CATEGORY_NODES.some((node) => node.slug === slug);
  const isHomeTab = HOME_CATEGORY_NODE.children?.some((child) => child.slug === slug) ?? false;
  return isRoutable && !isHomeTab;
}

export function resolveCategoryFeedPrefix(slug: CategorySlug) {
  return slug === "epl" ? "sport-football-epl" : slug;
}

export function isHiddenNode(node: CategoryNode) {
  return !("routable" in node) || (!node.routable && !node.redirectPath);
}

function processGraph<T extends CategoryNode>(graph: ReadonlyArray<T>): ReadonlyArray<T> {
  return graph.map((node) => {
    return Object.create({
      slug: node.slug,
      routable: typeof node.routable !== "boolean" && !BLOCKED_SLUG_SET.has(node.slug),
      redirectPath: node.redirectPath,
      children: node.children && processGraph(node.children),
    });
  });
}
