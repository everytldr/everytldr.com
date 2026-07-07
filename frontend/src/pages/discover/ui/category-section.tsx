import { ArticleCardSkeleton, ArticleList } from "@/entities/article";
import type { MainCategoryNode } from "@/shared/config";
import { Link, type Locale } from "@/shared/i18n";
import { buildCategoryUrl, cn } from "@/shared/lib";
import { Skeleton, Translation } from "@/shared/ui";
import { range } from "lodash-es";
import { connection } from "next/server";
import { fetchArticles } from "../api/fetch-articles";

export const CATEGORY_SECTION_SIZE = 4;

type CategorySectionProps = {
  className?: string;
  node: MainCategoryNode;
  locale: Locale;
};

export async function CategorySection({ className, node, locale }: CategorySectionProps) {
  await connection();

  const articles = await fetchArticles(node.slug, locale, CATEGORY_SECTION_SIZE);

  return (
    <section
      className={cn(
        "rounded-md border border-hairline bg-canvas p-lg dark:bg-surface-soft",
        className,
      )}
    >
      <div className="mb-sm flex items-center justify-between gap-sm">
        <Translation
          className="text-display-md text-ink"
          as="h2"
          tKey={`header.category.${node.slug}`}
        />
        <Link
          className="shrink-0 text-button-sm text-primary hover:underline"
          href={buildCategoryUrl(node)}
          prefetch={false}
        >
          <Translation tKey="common.see-all" />
        </Link>
      </div>
      <ArticleList
        listClassName="grid grid-cols-1 gap-x-lg md:grid-cols-2"
        articles={articles}
        empty={null}
      />
    </section>
  );
}

type CategorySectionSkeletonProps = {
  className?: string;
  count: number;
};

export function CategorySectionSkeleton({ className, count }: CategorySectionSkeletonProps) {
  return (
    <section
      className={cn(
        "rounded-md border border-hairline bg-canvas p-lg dark:bg-surface-soft",
        className,
      )}
    >
      <div className="mb-sm flex items-center justify-between gap-sm">
        <Skeleton className="w-32 text-display-md">&nbsp;</Skeleton>
        <Skeleton className="w-16 text-button-sm">&nbsp;</Skeleton>
      </div>
      <ul className="grid grid-cols-1 gap-x-lg md:grid-cols-2">
        {range(count).map((i) => (
          <li key={i}>
            <ArticleCardSkeleton />
          </li>
        ))}
      </ul>
    </section>
  );
}
