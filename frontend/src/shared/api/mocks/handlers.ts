import { http } from "msw";
import {
  createArticleComment,
  getArticle,
  getMyArticleLike,
  likeArticle,
  listArticleComments,
  listArticles,
  unlikeArticle,
} from "./fetchers/article";

export const handlers = [
  http.get("*/api/articles", listArticles),
  http.get("*/api/articles/:articleId/comments", listArticleComments),
  http.post("*/api/articles/:articleId/comments", createArticleComment),
  http.get("*/api/articles/:articleId/likes/me", getMyArticleLike),
  http.put("*/api/articles/:articleId/likes/me", likeArticle),
  http.delete("*/api/articles/:articleId/likes/me", unlikeArticle),
  http.get("*/api/articles/:id", getArticle),
];
