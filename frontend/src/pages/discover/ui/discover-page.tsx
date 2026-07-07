import type { ArticleListItem } from "@/shared/api";
import { listArticles } from "@/shared/api";
import { ROUTABLE_MAIN_CATEGORY_NODES } from "@/shared/config";
import type { Locale } from "@/shared/i18n";
import { cn, type Optional } from "@/shared/lib";
import { Container } from "@/shared/ui";
import { cacheLife, cacheTag } from "next/cache";
import { CategorySection } from "./category-section";
import { LatestSection } from "./latest-section";

type DiscoverPageProps = {
  className?: string;
  locale: Locale;
};

const CATEGORY_SECTION_SIZE = 4;
const LATEST_SECTION_SIZE = 10;

export async function DiscoverPage({ className, locale }: DiscoverPageProps) {
  const [sections, latest] = await Promise.all([
    Promise.all(
      ROUTABLE_MAIN_CATEGORY_NODES.map(async (node) => ({
        node,
        articles: await fetchArticles(node.slug, locale, CATEGORY_SECTION_SIZE),
      })),
    ),
    fetchArticles(undefined, locale, LATEST_SECTION_SIZE),
  ]);

  return (
    <main className={cn("py-lg", className)}>
      <Container className="grid grid-cols-1 gap-lg lg:grid-cols-3">
        <div className="space-y-lg lg:col-span-2">
          {sections
            .filter(({ articles }) => articles.length > 0)
            .map(({ node, articles }) => (
              <CategorySection key={node.slug} node={node} articles={articles} />
            ))}
        </div>
        <div className="lg:col-span-1">
          <LatestSection
            className="lg:sticky lg:top-[calc(var(--floating-subnav-height)+var(--spacing-sm))] lg:transition-[top] lg:duration-200 lg:ease-out"
            articles={latest}
          />
        </div>
      </Container>
    </main>
  );
}

async function fetchArticles(
  categoryPrefix: Optional<string>,
  locale: Locale,
  size: number,
): Promise<ArticleListItem[]> {
  "use cache";

  cacheLife("minutes");
  cacheTag(`articles:${locale}:${categoryPrefix ?? "latest"}`);

  const response = await listArticles(
    { categoryPrefix, size },
    { headers: { "Accept-Language": locale } },
  );

  return response.status === 200 ? (response.data.items ?? []) : [];
}
