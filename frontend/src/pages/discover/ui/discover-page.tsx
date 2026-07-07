import { ArticleCardSkeleton } from "@/entities/article";
import { ROUTABLE_MAIN_CATEGORY_NODES } from "@/shared/config";
import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Skeleton } from "@/shared/ui";
import { range } from "lodash-es";
import { Suspense } from "react";
import { CATEGORY_SECTION_SIZE, CategorySection } from "./category-section";
import { LATEST_SECTION_SIZE, LatestSection } from "./latest-section";

type DiscoverPageProps = {
  className?: string;
  locale: Locale;
};

export function DiscoverPage({ className, locale }: DiscoverPageProps) {
  return (
    <main className={cn("py-lg", className)}>
      <Container className="grid grid-cols-1 gap-lg lg:grid-cols-3">
        <div className="space-y-lg lg:col-span-2">
          <Suspense
            fallback={range(3).map((i) => (
              <CategorySectionSkeleton key={i} count={CATEGORY_SECTION_SIZE} />
            ))}
          >
            {ROUTABLE_MAIN_CATEGORY_NODES.map((node) => (
              <CategorySection key={node.slug} node={node} locale={locale} />
            ))}
          </Suspense>
        </div>
        <div className="lg:sticky lg:top-[calc(var(--floating-subnav-height)+var(--spacing-sm))] lg:col-span-1 lg:transition-[top] lg:duration-200 lg:ease-out">
          <Suspense fallback={<LatestSectionSkeleton count={LATEST_SECTION_SIZE} />}>
            <LatestSection locale={locale} />
          </Suspense>
        </div>
      </Container>
    </main>
  );
}

type CategorySectionSkeletonProps = {
  className?: string;
  count: number;
};

function CategorySectionSkeleton({ className, count }: CategorySectionSkeletonProps) {
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

type LatestSectionSkeletonProps = {
  className?: string;
  count: number;
};

function LatestSectionSkeleton({ className, count }: LatestSectionSkeletonProps) {
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
