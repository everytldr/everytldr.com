import type { BriefingListItem } from "@/shared/api";
import { Link, type Locale } from "@/shared/i18n";
import {
  buildBriefingDetailUrl,
  cn,
  formatNumericMonthDay,
  formatWeekday,
  markdownToPlainText,
} from "@/shared/lib";
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
        "group flex min-w-0 gap-md rounded-sm py-md outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas",
        className,
      )}
      href={buildBriefingDetailUrl(briefing.date)}
      prefetch={false}
    >
      <time className="flex w-14 shrink-0 flex-col items-start pt-2xs" dateTime={briefing.date}>
        <span className="text-caption-mono text-ink">{formatNumericMonthDay(briefing.date)}</span>
        <span className="text-caption text-meta">{formatWeekday(briefing.date, locale)}</span>
      </time>
      <div className="min-w-0 space-y-2xs">
        <h2 className="line-clamp-2 text-display-sm text-ink group-hover:text-primary">
          {briefing.title}
        </h2>
        <p className="line-clamp-2 text-body-sm text-meta">
          {markdownToPlainText(briefing.excerpt)}
        </p>
      </div>
    </Link>
  );
}

type BriefingRowSkeletonProps = {
  className?: string;
};

export function BriefingRowSkeleton({ className }: BriefingRowSkeletonProps) {
  return (
    <div className={cn("flex gap-md py-md", className)}>
      <div className="w-14 shrink-0 space-y-2xs pt-2xs">
        <Skeleton className="w-10 text-caption-mono">&nbsp;</Skeleton>
        <Skeleton className="w-8 text-caption">&nbsp;</Skeleton>
      </div>
      <div className="min-w-0 flex-1 space-y-2xs">
        <Skeleton className="w-3/4 text-display-sm">&nbsp;</Skeleton>
        <Skeleton className="w-full text-body-sm">&nbsp;</Skeleton>
      </div>
    </div>
  );
}
