"use client";

import { cn } from "@/shared/lib";
import { Input } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useState, type SubmitEvent } from "react";
import { useArrowNavigation } from "../lib/use-arrow-navigation";
import { PopularSearches } from "./popular-searches";
import { RecentSearches } from "./recent-searches";

type SearchPanelProps = {
  className?: string;
  initialQuery?: string;
  recentTerms: ReadonlyArray<string>;
  showExploreSections?: boolean;
  onItemSelect: (term: string) => void;
  onRemoveRecent: (term: string) => void;
  onSubmit: (query: string) => void;
};

export function SearchPanel({
  className,
  initialQuery = "",
  recentTerms,
  showExploreSections = true,
  onItemSelect,
  onRemoveRecent,
  onSubmit,
}: SearchPanelProps) {
  const t = useTranslations("search");
  const [query, setQuery] = useState(initialQuery);

  useArrowNavigation();

  return (
    <div className={cn("flex flex-col gap-lg", className)}>
      <form onSubmit={handleSubmit}>
        <Input
          variant="search"
          autoComplete="off"
          enterKeyHint="search"
          name="q"
          placeholder={t("input-placeholder")}
          type="search"
          value={query}
          aria-label={t("aria-label.search-input")}
          data-search-nav
          onChange={(event) => setQuery(event.target.value)}
        />
      </form>
      {showExploreSections && (
        <>
          {recentTerms.length > 0 && (
            <RecentSearches
              terms={recentTerms}
              onItemSelect={onItemSelect}
              onRemove={onRemoveRecent}
            />
          )}
          <PopularSearches onItemSelect={onItemSelect} />
        </>
      )}
    </div>
  );

  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit(query);
  }
}
