import { http } from "msw";
import { getArticle, listArticles } from "./fetchers/article";

export const handlers = [
  http.get("*/api/articles", listArticles),
  http.get("*/api/articles/:id", getArticle),
];
