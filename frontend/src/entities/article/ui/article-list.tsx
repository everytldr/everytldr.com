import type { ArticleListItem } from "@/shared/api";
import { type PropsWithChildren, type ReactNode } from "react";
import { ArticleCard } from "./article-card";

type ArticleListProps = PropsWithChildren<{
  className?: string;
  articles: ArticleListItem[];
  empty: ReactNode;
}>;

export function ArticleList({ className, articles, empty, children }: ArticleListProps) {
  return (
    <div className={className}>
      {articles.length === 0 ? (
        empty
      ) : (
        <ul>
          {articles.map((article) => (
            <li key={article.id}>
              <ArticleCard article={article} />
            </li>
          ))}
          {children}
        </ul>
      )}
    </div>
  );
}
