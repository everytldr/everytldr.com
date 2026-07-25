import { Link, type Locale } from "@/shared/i18n";
import type { Nullable } from "@/shared/lib";
import { buildBriefingDetailUrl, cn, formatDate, formatMonthDay } from "@/shared/lib";
import { Skeleton } from "@/shared/ui";
import { range } from "lodash-es";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { getTranslations } from "next-intl/server";

type BriefingDateNavProps = {
  className?: string;
  previousDate: Nullable<string>;
  nextDate: Nullable<string>;
  locale: Locale;
};

export async function BriefingDateNav({
  className,
  previousDate,
  nextDate,
  locale,
}: BriefingDateNavProps) {
  const t = await getTranslations({ locale, namespace: "briefings" });

  return (
    <nav
      className={cn("flex items-start justify-between gap-sm", className)}
      aria-label={t("date-nav-label")}
    >
      {previousDate ? (
        <AdjacentBriefingLink
          date={previousDate}
          direction="previous"
          label={t("previous")}
          locale={locale}
        />
      ) : (
        <span className="w-9" />
      )}

      {nextDate ? (
        <AdjacentBriefingLink date={nextDate} direction="next" label={t("next")} locale={locale} />
      ) : (
        <span className="w-9" />
      )}
    </nav>
  );
}

type AdjacentBriefingLinkProps = {
  className?: string;
  date: string;
  direction: "previous" | "next";
  label: string;
  locale: Locale;
};

function AdjacentBriefingLink({
  className,
  date,
  direction,
  label,
  locale,
}: AdjacentBriefingLinkProps) {
  const Icon = direction === "previous" ? ChevronLeft : ChevronRight;

  return (
    <Link
      className={cn("group flex flex-col items-center gap-2xs outline-none", className)}
      href={buildBriefingDetailUrl(date)}
      prefetch={false}
      aria-label={`${label}: ${formatDate(date, locale)}`}
    >
      <span className="inline-flex size-9 items-center justify-center rounded-full bg-surface-soft p-xs text-ink transition-colors group-hover:bg-surface-strong group-focus-visible:ring-2 group-focus-visible:ring-primary group-active:bg-surface-pressed">
        <Icon className="pointer-events-none size-4.5" aria-hidden="true" />
      </span>
      <time
        className="text-caption text-meta transition-colors group-hover:text-primary"
        dateTime={date}
      >
        {formatMonthDay(date, locale)}
      </time>
    </Link>
  );
}

type BriefingDateNavSkeletonProps = {
  className?: string;
};

export function BriefingDateNavSkeleton({ className }: BriefingDateNavSkeletonProps) {
  return (
    <div
      className={cn(
        "flex items-start justify-between gap-sm border-t border-hairline-soft pt-lg",
        className,
      )}
    >
      {range(2).map((i) => (
        <div key={i} className="flex flex-col items-center gap-2xs">
          <Skeleton className="size-9 rounded-full" />
          <Skeleton className="w-10 text-caption">&nbsp;</Skeleton>
        </div>
      ))}
    </div>
  );
}
