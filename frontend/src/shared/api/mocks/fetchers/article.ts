import type { ArticleDetailResponse, ArticleListItem, ArticleListResponse } from "@/shared/api";
import { EplTeam } from "@/shared/config";
import { drop, take, times } from "lodash-es";
import { HttpResponse } from "msw";

const EPL_TEAMS = Object.values(EplTeam);

const ALL_ARTICLES: ArticleListItem[] = times(100, (index) => {
  const team = EPL_TEAMS[index % EPL_TEAMS.length];
  return {
    id: `article-${index + 1}`,
    title: `Article Title ${index + 1}: ${team.toUpperCase()} News and Match Updates`,
    summary: `This is a beautifully generated mock summary for article ${index + 1}.`,
    source: `Source ${(index % 3) + 1}`,
    category: `sport-football-epl-${team}`,
    publishedAt: new Date(Date.now() - index * 60 * 60 * 1000).toISOString(),
    thumbnailUrl: `https://picsum.photos/seed/article-${index + 1}/300/200`,
  };
});

export const listArticles = ({ request }: { request: Request }) => {
  const url = new URL(request.url);
  const cursor = url.searchParams.get("cursor");
  const categoryPrefix = url.searchParams.get("categoryPrefix");
  const size = Number(url.searchParams.get("size") ?? "10");

  const startIndex = cursor ? parseInt(cursor, 10) : 0;

  const filteredArticles = categoryPrefix
    ? ALL_ARTICLES.filter((article) =>
        article.category.toLowerCase().startsWith(categoryPrefix.toLowerCase()),
      )
    : ALL_ARTICLES;

  const items = take(drop(filteredArticles, startIndex), size);

  const nextIndex = startIndex + size;
  const nextCursor = nextIndex < filteredArticles.length ? nextIndex.toString() : undefined;

  const responseData: ArticleListResponse = {
    items,
    nextCursor,
  };

  return HttpResponse.json(responseData);
};

export const getArticle = ({ params }: { params: { id: string } }) => {
  const { id } = params;

  const article = ALL_ARTICLES.find((a) => a.id === `article-${id}` || a.id === id);

  if (!article) {
    return new HttpResponse(null, { status: 404 });
  }

  const responseData: ArticleDetailResponse = {
    id: parseInt(id),
    title: article.title,
    summary: article.summary,
    category: article.category,
    source: article.source,
    publishedAt: article.publishedAt,
    thumbnailUrl: article.thumbnailUrl ?? undefined,
    commentCount: 12,
    likeCount: 42,
    sourceUrl: "https://example.com",
  };

  return HttpResponse.json(responseData);
};
