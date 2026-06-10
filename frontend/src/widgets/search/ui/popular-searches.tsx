"use client";

import { EPL_BIG_SIX_TEAMS } from "@/shared/config";
import { cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { CornerDownLeft } from "lucide-react";
import { useTranslations } from "next-intl";

type PopularSearchesProps = {
  className?: string;
  onItemSelect: (term: string) => void;
};

export function PopularSearches({ className, onItemSelect }: PopularSearchesProps) {
  const t = useTranslations();

  return (
    <section className={cn("flex flex-col gap-sm", className)}>
      <header className="flex items-baseline justify-between">
        <Translation className="text-title-sm text-ink" as="h3" tKey="search.section.popular" />
        <span className="text-caption text-meta-soft">{t("search.popular-meta")}</span>
      </header>
      <ul className="flex flex-col">
        {EPL_BIG_SIX_TEAMS.map((team, index) => {
          const label = t(`epl.team.${team}`);
          return (
            <li key={team}>
              <PopularRow rank={index + 1} topic={label} onSelect={() => onItemSelect(label)} />
            </li>
          );
        })}
      </ul>
    </section>
  );
}

type PopularRowProps = {
  className?: string;
  rank: number;
  topic: string;
  onSelect: () => void;
};

function PopularRow({ className, rank, topic, onSelect }: PopularRowProps) {
  return (
    <button
      className={cn(
        "group flex w-full cursor-pointer items-center gap-md rounded-sm px-sm py-2xs text-left transition-colors outline-none hover:bg-surface-soft focus-visible:bg-surface-soft focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong dark:hover:bg-surface-strong dark:focus-visible:bg-surface-strong dark:active:bg-surface-pressed",
        className,
      )}
      type="button"
      data-search-nav
      onClick={onSelect}
    >
      <span className="w-md shrink-0 text-caption-mono text-meta tabular-nums">{rank}</span>
      <span className="flex-1 truncate text-body-md text-ink">{topic}</span>
      <CornerDownLeft className="size-sm shrink-0 text-meta-soft opacity-0 transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100" />
    </button>
  );
}
