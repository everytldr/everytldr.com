import { type ArticleListItem, listArticles } from "@/shared/api";
import { SITE_NAME, SITE_URL } from "@/shared/config";
import { getPathname, isLocale, type Locale } from "@/shared/i18n";
import { buildArticleFeedItem, buildRssFeed } from "@/shared/lib";
import { getTranslations } from "next-intl/server";
import { cacheLife, cacheTag } from "next/cache";

const FEED_ITEM_COUNT = 20;

type RouteContext = { params: Promise<{ locale: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  const { locale } = await params;
  if (!isLocale(locale)) {
    return new Response(null, { status: 404 });
  }

  const t = await getTranslations({ locale, namespace: "metadata.default" });
  const articles = await fetchFeedArticles(locale);

  const xml = buildRssFeed({
    title: SITE_NAME,
    description: t("description"),
    link: `${SITE_URL}${getPathname({ locale, href: "/" })}`,
    feedUrl: `${SITE_URL}/${locale}/feed.xml`,
    language: locale,
    items: articles.map((article) => buildArticleFeedItem(article, locale)),
  });

  return new Response(xml, {
    headers: { "content-type": "application/rss+xml; charset=utf-8" },
  });
}

async function fetchFeedArticles(locale: Locale): Promise<ArticleListItem[]> {
  "use cache";

  cacheLife("minutes");
  cacheTag(`feed:${locale}`);

  try {
    const response = await listArticles(
      { size: FEED_ITEM_COUNT },
      { headers: { "Accept-Language": locale } },
    );
    return response.status === 200 ? response.data.items : [];
  } catch (error) {
    console.error("Failed to fetch articles for feed", error);
    return [];
  }
}
