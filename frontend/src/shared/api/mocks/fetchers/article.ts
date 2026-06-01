import type {
  ArticleCommentCreateRequest,
  ArticleCommentListItem,
  ArticleCommentListResponse,
  ArticleDetailResponse,
  ArticleLikeStateResponse,
  ArticleListItem,
  ArticleListResponse,
} from "@/shared/api";
import { EplTeam } from "@/shared/config";
import { AN_HOUR, type Optional } from "@/shared/lib";
import { drop, take, times } from "lodash-es";
import { HttpResponse, type HttpResponseResolver } from "msw";

const EPL_TEAMS = Object.values(EplTeam);
const DEFAULT_LIKE_COUNT = 42;
const FIRST_ARTICLE_ID = "45660871069790209";

const ALL_ARTICLES: ArticleListItem[] = times(100, (index) => {
  const team = EPL_TEAMS[index % EPL_TEAMS.length];
  const id = (BigInt(FIRST_ARTICLE_ID) + BigInt(index)).toString();
  return {
    id,
    title: `Article Title ${index + 1}: ${team.toUpperCase()} News and Match Updates`,
    summary: `This is a beautifully generated mock summary for article ${index + 1}.`,
    source: `Source ${(index % 3) + 1}`,
    category: `sport-football-epl-${team}`,
    publishedAt: new Date(Date.now() - index * AN_HOUR).toISOString(),
    thumbnailUrl: `https://picsum.photos/seed/article-${id}/300/200`,
  };
});

const COMMENTS_BY_ARTICLE_ID = new Map<string, ArticleCommentListItem[]>(
  ALL_ARTICLES.map((article, index) => {
    const firstCommentId = (BigInt(article.id) + BigInt(1)).toString();
    const secondCommentId = (BigInt(article.id) + BigInt(2)).toString();
    return [
      article.id,
      [
        {
          id: firstCommentId,
          content: `Helpful summary. This made article ${index + 1} much easier to scan.`,
          createdAt: new Date(Date.now() - AN_HOUR).toISOString(),
          maskedIp: "203.0.113.*",
          nickname: "Reader One",
          parentId: null,
        },
        {
          id: secondCommentId,
          content: "I would like to see more context on the source article next.",
          createdAt: new Date(Date.now() - AN_HOUR / 2).toISOString(),
          maskedIp: "198.51.100.*",
          nickname: "Football Fan",
          parentId: firstCommentId,
        },
      ],
    ];
  }),
);

const LIKED_ARTICLE_IDS = new Set<string>();

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

export const getArticle = ({ params: { id } }: { params: { id: string } }) => {
  const article = findArticle(id);

  if (!article) {
    return new HttpResponse(null, { status: 404 });
  }

  const commentCount = getArticleComments(id).length;
  const likeCount = getArticleLikeCount(id);
  const sourceUrl = "https://example.com";

  const responseData: ArticleDetailResponse = {
    ...article,
    thumbnailUrl: article.thumbnailUrl ?? undefined,
    commentCount,
    likeCount,
    sourceUrl,
  };

  return HttpResponse.json(responseData);
};

export const listArticleComments = ({
  params: { articleId },
}: {
  params: { articleId: string };
}) => {
  if (!findArticle(articleId)) {
    return new HttpResponse(null, { status: 404 });
  }

  const items = getArticleComments(articleId);
  const responseData: ArticleCommentListResponse = { items };

  return HttpResponse.json(responseData);
};

export const createArticleComment: HttpResponseResolver<
  { articleId: string },
  ArticleCommentCreateRequest
> = async ({ request, params: { articleId } }) => {
  if (!findArticle(articleId)) {
    return new HttpResponse(null, { status: 404 });
  }

  const body = await request.json();
  const comments = getArticleComments(articleId);

  const newComment: ArticleCommentListItem = {
    ...body,
    id: (BigInt(articleId) + BigInt(1000 + comments.length)).toString(),
    createdAt: new Date().toISOString(),
    maskedIp: "192.0.2.*",
  };

  comments.push(newComment);
  COMMENTS_BY_ARTICLE_ID.set(articleId, comments);

  return HttpResponse.json(newComment, { status: 201 });
};

export const getMyArticleLike = ({ params: { articleId } }: { params: { articleId: string } }) => {
  if (!findArticle(articleId)) {
    return new HttpResponse(null, { status: 404 });
  }

  return HttpResponse.json(buildArticleLikeState(articleId));
};

export const likeArticle = ({ params: { articleId } }: { params: { articleId: string } }) => {
  if (!findArticle(articleId)) {
    return new HttpResponse(null, { status: 404 });
  }

  LIKED_ARTICLE_IDS.add(articleId);

  return HttpResponse.json(buildArticleLikeState(articleId));
};

export const unlikeArticle = ({ params: { articleId } }: { params: { articleId: string } }) => {
  if (!findArticle(articleId)) {
    return new HttpResponse(null, { status: 404 });
  }

  LIKED_ARTICLE_IDS.delete(articleId);

  return HttpResponse.json(buildArticleLikeState(articleId));
};

function findArticle(articleId: string): Optional<ArticleListItem> {
  return ALL_ARTICLES.find((article) => article.id === articleId);
}

function getArticleComments(articleId: string): ArticleCommentListItem[] {
  return COMMENTS_BY_ARTICLE_ID.get(articleId) ?? [];
}

function getArticleLikeCount(articleId: string): number {
  return DEFAULT_LIKE_COUNT + (LIKED_ARTICLE_IDS.has(articleId) ? 1 : 0);
}

function buildArticleLikeState(articleId: string): ArticleLikeStateResponse {
  return {
    articleId,
    likeCount: getArticleLikeCount(articleId),
    likedByReader: LIKED_ARTICLE_IDS.has(articleId),
  };
}
