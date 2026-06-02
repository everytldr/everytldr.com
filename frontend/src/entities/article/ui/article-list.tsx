import type { ArticleListItem } from "@/shared/api";
import { Link } from "@/shared/i18n";
import { buildArticleDetailUrl } from "@/shared/lib";
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
              <Link
                className="group block outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas"
                href={buildArticleDetailUrl(article.id)}
              >
                <ArticleCard titleClassName="group-hover:text-primary" article={article} />
              </Link>
            </li>
          ))}
          {children}
        </ul>
      )}
    </div>
  );
}
