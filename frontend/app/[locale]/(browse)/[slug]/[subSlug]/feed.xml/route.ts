import { type ArticleListItem, listArticles } from "@/shared/api";
import { EPL_TEAMS_ALPHABETICAL, EplTabSlug, SITE_NAME, SITE_URL } from "@/shared/config";
import { getPathname, isLocale, type Locale } from "@/shared/i18n";
import { buildArticleFeedItem, buildRssFeed } from "@/shared/lib";
import { getTranslations } from "next-intl/server";
import { cacheLife, cacheTag } from "next/cache";
import { permanentRedirect } from "next/navigation";

const FEED_ITEM_COUNT = 20;

type RouteContext = { params: Promise<{ locale: string; slug: string; subSlug: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  const { locale, slug, subSlug } = await params;
  if (!isLocale(locale) || slug !== "epl") {
    return new Response(null, { status: 404 });
  }

  if (subSlug === EplTabSlug.News) {
    permanentRedirect(`/${locale}/epl/feed.xml`);
  }

  const team = EPL_TEAMS_ALPHABETICAL.find((team) => team === subSlug);
  if (team === undefined) {
    return new Response(null, { status: 404 });
  }

  const t = await getTranslations({ locale });
  const category = `${t("header.subcategory.epl")} · ${t(`epl.team.${team}`)}`;
  const tMeta = await getTranslations({ locale, namespace: "metadata.category" });
  const categoryPrefix = `sport-football-epl-${team}`;
  const articles = await fetchEplTeamFeedArticles(categoryPrefix, locale);

  const xml = buildRssFeed({
    title: `${SITE_NAME} · ${category}`,
    description: tMeta("description", { category }),
    link: `${SITE_URL}${getPathname({ locale, href: `/epl/${team}` })}`,
    feedUrl: `${SITE_URL}/${locale}/epl/${team}/feed.xml`,
    language: locale,
    items: articles.map((article) => buildArticleFeedItem(article, locale)),
  });

  return new Response(xml, {
    headers: { "content-type": "application/rss+xml; charset=utf-8" },
  });
}

async function fetchEplTeamFeedArticles(
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
    console.error("Failed to fetch articles for EPL team feed", error);
    return [];
  }
}
