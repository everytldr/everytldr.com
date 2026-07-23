import type { BriefingListItem } from "@/shared/api";
import { Link, type Locale } from "@/shared/i18n";
import { buildBriefingDetailUrl, cn, formatDate } from "@/shared/lib";
import { Skeleton } from "@/shared/ui";

type BriefingRowProps = {
  className?: string;
  briefing: BriefingListItem;
  locale: Locale;
};

export function BriefingRow({ className, briefing, locale }: BriefingRowProps) {
  return (
    <Link
      className={cn(
        "group flex min-w-0 flex-col gap-2xs rounded-md border border-hairline bg-canvas p-lg outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas dark:bg-surface-soft",
        className,
      )}
      href={buildBriefingDetailUrl(briefing.date)}
      prefetch={false}
    >
      <time className="text-caption text-meta" dateTime={briefing.date}>
        {formatDate(briefing.date, locale)}
      </time>
      <h2 className="line-clamp-2 min-w-0 text-display-sm text-ink group-hover:text-primary">
        {briefing.title}
      </h2>
    </Link>
  );
}

type BriefingRowSkeletonProps = {
  className?: string;
};

export function BriefingRowSkeleton({ className }: BriefingRowSkeletonProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-2xs rounded-md border border-hairline bg-canvas p-lg dark:bg-surface-soft",
        className,
      )}
    >
      <Skeleton className="w-24 text-caption">&nbsp;</Skeleton>
      <Skeleton className="w-3/4 text-display-sm">&nbsp;</Skeleton>
    </div>
  );
}
