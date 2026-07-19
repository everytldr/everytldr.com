"use client";

import { MIN_SEARCH_QUERY_LENGTH } from "@/shared/config";
import { cn } from "@/shared/lib";
import { Input, toast } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useEffect, useState, type SubmitEvent } from "react";
import { useArrowNavigation } from "../lib/use-arrow-navigation";
import { HotArticles } from "./hot-articles";
import { RecentSearches } from "./recent-searches";

type SearchPanelProps = {
  className?: string;
  initialQuery?: string;
  recentTerms: ReadonlyArray<string>;
  showExploreSections?: boolean;
  onArticleSelect: (articleId: string) => void;
  onItemSelect: (term: string) => void;
  onRemoveRecent: (term: string) => void;
  onSubmit: (query: string) => void;
};

export function SearchPanel({
  className,
  initialQuery = "",
  recentTerms,
  showExploreSections = true,
  onArticleSelect,
  onItemSelect,
  onRemoveRecent,
  onSubmit,
}: SearchPanelProps) {
  const t = useTranslations("search");
  const [query, setQuery] = useState(initialQuery);

  const navigationRef = useArrowNavigation();

  useEffect(() => {
    if (query === initialQuery) {
      return;
    }

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setQuery(initialQuery);
  }, [initialQuery]);

  return (
    <div ref={navigationRef} className={cn("flex flex-col gap-lg", className)}>
      <form onSubmit={handleSubmit}>
        <Input
          variant="search"
          autoComplete="off"
          enterKeyHint="search"
          name="q"
          placeholder={t("input-placeholder", { min: MIN_SEARCH_QUERY_LENGTH })}
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
          <HotArticles onArticleSelect={onArticleSelect} />
        </>
      )}
    </div>
  );

  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (query.trim().length < MIN_SEARCH_QUERY_LENGTH) {
      toast(t("min-length-toast", { min: MIN_SEARCH_QUERY_LENGTH }));
      return;
    }
    onSubmit(query);
  }
}
