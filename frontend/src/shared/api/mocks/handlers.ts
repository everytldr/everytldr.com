import { http } from "msw";
import {
  countArticleView,
  createArticleComment,
  deleteArticleComment,
  editArticleComment,
  getArticle,
  getMyArticleLike,
  likeArticle,
  listArticleComments,
  listArticles,
  listNewsSitemapArticles,
  listSitemapArticles,
  searchArticles,
  unlikeArticle,
  verifyArticleCommentPassword,
} from "./fetchers/article";
import {
  getArticleBriefing,
  getBriefing,
  listBriefings,
  listSitemapBriefings,
} from "./fetchers/briefing";

export const handlers = [
  http.get("*/api/briefings", listBriefings),
  http.get("*/api/briefings/:date", getBriefing),
  http.get("*/api/articles", listArticles),
  http.get("*/internal/sitemap/articles", listSitemapArticles),
  http.get("*/internal/sitemap/briefings", listSitemapBriefings),
  http.get("*/internal/sitemap/news", listNewsSitemapArticles),
  http.get("*/api/articles/search", searchArticles),
  http.get("*/api/articles/:articleId/comments", listArticleComments),
  http.post("*/api/articles/:articleId/comments", createArticleComment),
  http.patch("*/api/articles/:articleId/comments/:commentId", editArticleComment),
  http.delete("*/api/articles/:articleId/comments/:commentId", deleteArticleComment),
  http.post(
    "*/api/articles/:articleId/comments/:commentId/password-verification",
    verifyArticleCommentPassword,
  ),
  http.post("*/api/articles/:articleId/views", countArticleView),
  http.get("*/api/articles/:articleId/briefing", getArticleBriefing),
  http.get("*/api/articles/:articleId/likes/me", getMyArticleLike),
  http.put("*/api/articles/:articleId/likes/me", likeArticle),
  http.delete("*/api/articles/:articleId/likes/me", unlikeArticle),
  http.get("*/api/articles/:id", getArticle),
];
