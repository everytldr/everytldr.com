import { Link, type Locale } from "@/shared/i18n";
import { buildBriefingDetailUrl, cn, formatDate } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { Newspaper } from "lucide-react";
import { fetchArticleBriefing } from "../api/fetch-article-briefing";

type ArticleBriefingLinkProps = {
  className?: string;
  articleId: string;
  locale: Locale;
};

export async function ArticleBriefingLink({
  className,
  articleId,
  locale,
}: ArticleBriefingLinkProps) {
  const briefing = await fetchArticleBriefing(articleId, locale);

  if (!briefing) {
    return null;
  }

  return (
    <Link
      className={cn(
        "group flex items-center gap-xs rounded-md border border-hairline bg-canvas px-md py-sm outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas dark:bg-surface-soft",
        className,
      )}
      href={buildBriefingDetailUrl(briefing.date)}
      prefetch={false}
    >
      <Newspaper className="size-sm shrink-0 text-primary" aria-hidden="true" />
      <span className="min-w-0 text-body-sm text-meta group-hover:text-primary">
        <Translation
          tKey="article-detail.briefing-link"
          values={{ date: formatDate(briefing.date, locale) }}
        />
      </span>
    </Link>
  );
}
