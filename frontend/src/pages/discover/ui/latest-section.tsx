import { ArticleList } from "@/entities/article";
import type { ArticleListItem } from "@/shared/api";
import { Link } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";

type LatestSectionProps = {
  className?: string;
  articles: ArticleListItem[];
};

export function LatestSection({ className, articles }: LatestSectionProps) {
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
