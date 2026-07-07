import { ArticleList } from "@/entities/article";
import { Link, type Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { connection } from "next/server";
import { fetchArticles } from "../api/fetch-articles";

export const LATEST_SECTION_SIZE = 10;

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
