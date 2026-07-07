import { ROUTABLE_MAIN_CATEGORY_NODES } from "@/shared/config";
import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container } from "@/shared/ui";
import { range } from "lodash-es";
import { Suspense } from "react";
import {
  CATEGORY_SECTION_SIZE,
  CategorySection,
  CategorySectionSkeleton,
} from "./category-section";
import { LATEST_SECTION_SIZE, LatestSection, LatestSectionSkeleton } from "./latest-section";

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
        <div className="lg:sticky lg:top-[calc(var(--floating-subnav-height)+var(--spacing-sm))] lg:col-span-1 lg:self-start lg:transition-[top] lg:duration-200 lg:ease-out">
          <Suspense fallback={<LatestSectionSkeleton count={LATEST_SECTION_SIZE} />}>
            <LatestSection locale={locale} />
          </Suspense>
        </div>
      </Container>
    </main>
  );
}
