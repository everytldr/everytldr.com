import { Link, type Locale } from "@/shared/i18n";
import { buildBriefingDetailUrl, cn, formatDate } from "@/shared/lib";
import { Skeleton, Translation } from "@/shared/ui";
import { ArrowRight } from "lucide-react";
import { fetchLatestBriefing } from "../api/fetch-latest-briefing";

type BriefingHeroProps = {
  className?: string;
  locale: Locale;
};

export async function BriefingHero({ className, locale }: BriefingHeroProps) {
  const briefing = await fetchLatestBriefing(locale);

  if (!briefing) {
    return null;
  }

  return (
    <Link
      className={cn(
        "group block rounded-md border border-hairline bg-canvas p-lg outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas dark:bg-surface-soft",
        className,
      )}
      href={buildBriefingDetailUrl(briefing.date)}
      prefetch={false}
    >
      <div className="flex items-center justify-between gap-sm">
        <Translation
          className="text-caption font-medium text-primary"
          as="span"
          tKey="briefings.hero-eyebrow"
        />
        <time className="text-caption text-meta" dateTime={briefing.date}>
          {formatDate(briefing.date, locale)}
        </time>
      </div>
      <h2 className="mt-2xs text-display-md text-ink group-hover:text-primary">{briefing.title}</h2>
      <p className="mt-xs line-clamp-2 text-body-md text-meta">{briefing.excerpt}</p>
      <span className="mt-sm inline-flex items-center gap-2xs text-button-sm text-primary group-hover:underline">
        <Translation tKey="briefings.hero-cta" />
        <ArrowRight className="size-sm" aria-hidden="true" />
      </span>
    </Link>
  );
}

type BriefingHeroSkeletonProps = {
  className?: string;
};

export function BriefingHeroSkeleton({ className }: BriefingHeroSkeletonProps) {
  return (
    <div
      className={cn(
        "rounded-md border border-hairline bg-canvas p-lg dark:bg-surface-soft",
        className,
      )}
    >
      <Skeleton className="w-28 text-caption">&nbsp;</Skeleton>
      <Skeleton className="mt-2xs w-3/4 text-display-md">&nbsp;</Skeleton>
      <Skeleton className="mt-xs w-full text-body-md">&nbsp;</Skeleton>
      <Skeleton className="mt-2xs w-2/3 text-body-md">&nbsp;</Skeleton>
    </div>
  );
}
