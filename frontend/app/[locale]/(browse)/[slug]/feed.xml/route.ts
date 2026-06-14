import { type ArticleListItem, listArticles } from "@/shared/api";
import {
  isFeedableCategory,
  isMainCategorySlug,
  resolveCategoryFeedPrefix,
  SITE_NAME,
  SITE_URL,
} from "@/shared/config";
import { getPathname, isLocale, type Locale } from "@/shared/i18n";
import { buildArticleFeedItem, buildRssFeed } from "@/shared/lib";
import { getTranslations } from "next-intl/server";
import { cacheLife, cacheTag } from "next/cache";

const FEED_ITEM_COUNT = 20;

type RouteContext = { params: Promise<{ locale: string; slug: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  const { locale, slug } = await params;
  if (!isLocale(locale) || !isFeedableCategory(slug)) {
    return new Response(null, { status: 404 });
  }

  const category = isMainCategorySlug(slug)
    ? (await getTranslations({ locale, namespace: "header.category" }))(slug)
    : (await getTranslations({ locale, namespace: "header.subcategory" }))(slug);
  const tMeta = await getTranslations({ locale, namespace: "metadata.category" });

  const categoryPrefix = resolveCategoryFeedPrefix(slug);
  const articles = await fetchCategoryFeedArticles(categoryPrefix, locale);

  const xml = buildRssFeed({
    title: `${SITE_NAME} · ${category}`,
    description: tMeta("description", { category }),
    link: `${SITE_URL}${getPathname({ locale, href: `/${slug}` })}`,
    feedUrl: `${SITE_URL}/${locale}/${slug}/feed.xml`,
    language: locale,
    items: articles.map((article) => buildArticleFeedItem(article, locale)),
  });

  return new Response(xml, {
    headers: { "content-type": "application/rss+xml; charset=utf-8" },
  });
}

async function fetchCategoryFeedArticles(
  categoryPrefix: string,
  locale: Locale,
): Promise<ArticleListItem[]> {
  "use cache";

  cacheLife("minutes");
  cacheTag(`feed:${locale}:${categoryPrefix}`);

  try {
    const response = await listArticles(
      { categoryPrefix, size: FEED_ITEM_COUNT },
      { headers: { "Accept-Language": locale } },
    );
    return response.status === 200 ? response.data.items : [];
  } catch (error) {
    console.error("Failed to fetch articles for category feed", error);
    return [];
  }
}
