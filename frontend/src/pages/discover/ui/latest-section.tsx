import { ArticleCardSkeleton, ArticleList } from "@/entities/article";
import { Link, type Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Skeleton, Translation } from "@/shared/ui";
import { range } from "lodash-es";
import { connection } from "next/server";
import { fetchArticles } from "../api/fetch-articles";

export const LATEST_SECTION_SIZE = 6;

type LatestSectionProps = {
  className?: string;
  locale: Locale;
};

export async function LatestSection({ className, locale }: LatestSectionProps) {
  await connection();

  const articles = await fetchArticles(undefined, locale, LATEST_SECTION_SIZE);

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
          tKey="header.subcategory.latest"
        />
        <Link
          className="shrink-0 text-button-sm text-primary hover:underline"
          href="/latest"
          prefetch={false}
        >
          <Translation tKey="common.see-all" />
        </Link>
      </div>
      <ArticleList articles={articles} empty={null} />
    </section>
  );
}

type LatestSectionSkeletonProps = {
  className?: string;
  count: number;
};

export function LatestSectionSkeleton({ className, count }: LatestSectionSkeletonProps) {
  return (
    <section
      className={cn(
        "rounded-md border border-hairline bg-canvas p-lg dark:bg-surface-soft",
        className,
      )}
    >
      <div className="mb-sm flex items-center justify-between gap-sm">
        <Skeleton className="w-24 text-display-md">&nbsp;</Skeleton>
        <Skeleton className="w-16 text-button-sm">&nbsp;</Skeleton>
      </div>
      <ul>
        {range(count).map((i) => (
          <li key={i}>
            <ArticleCardSkeleton />
          </li>
        ))}
      </ul>
    </section>
  );
}
