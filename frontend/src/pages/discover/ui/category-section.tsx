import { ArticleList } from "@/entities/article";
import type { ArticleListItem } from "@/shared/api";
import type { MainCategoryNode } from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildCategoryUrl, cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";

type CategorySectionProps = {
  className?: string;
  node: MainCategoryNode;
  articles: ArticleListItem[];
};

export function CategorySection({ className, node, articles }: CategorySectionProps) {
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
