import type { BriefingListItem } from "@/shared/api";
import { Link, type Locale } from "@/shared/i18n";
import { buildBriefingDetailUrl, cn, formatDate, formatMonthDay } from "@/shared/lib";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { fetchAdjacentBriefings } from "../api/fetch-adjacent-briefings";

type BriefingDateNavProps = {
  className?: string;
  date: string;
  locale: Locale;
};

export async function BriefingDateNav({ className, date, locale }: BriefingDateNavProps) {
  const [{ previous, next }, t] = await Promise.all([
    fetchAdjacentBriefings(date, locale),
    getTranslations({ locale, namespace: "briefings" }),
  ]);

  if (!previous && !next) {
    return null;
  }

  return (
    <nav
      className={cn("flex items-start justify-between gap-sm", className)}
      aria-label={t("date-nav-label")}
    >
      {previous ? (
        <AdjacentBriefingLink
          briefing={previous}
          direction="previous"
          label={t("previous")}
          locale={locale}
        />
      ) : (
        <span className="w-9" />
      )}

      {next ? (
        <AdjacentBriefingLink briefing={next} direction="next" label={t("next")} locale={locale} />
      ) : (
        <span className="w-9" />
      )}
    </nav>
  );
}

type AdjacentBriefingLinkProps = {
  className?: string;
  briefing: BriefingListItem;
  direction: "previous" | "next";
  label: string;
  locale: Locale;
};

function AdjacentBriefingLink({
  className,
  briefing,
  direction,
  label,
  locale,
}: AdjacentBriefingLinkProps) {
  const Icon = direction === "previous" ? ChevronLeft : ChevronRight;

  return (
    <Link
      className={cn("group flex flex-col items-center gap-2xs outline-none", className)}
      href={buildBriefingDetailUrl(briefing.date)}
      prefetch={false}
      aria-label={`${label}: ${formatDate(briefing.date, locale)}`}
    >
      <span className="inline-flex size-9 items-center justify-center rounded-full bg-surface-soft p-xs text-ink transition-colors group-hover:bg-surface-strong group-focus-visible:ring-2 group-focus-visible:ring-primary group-active:bg-surface-pressed">
        <Icon className="pointer-events-none size-4.5" aria-hidden="true" />
      </span>
      <time
        className="text-caption text-meta transition-colors group-hover:text-primary"
        dateTime={briefing.date}
      >
        {formatMonthDay(briefing.date, locale)}
      </time>
    </Link>
  );
}
