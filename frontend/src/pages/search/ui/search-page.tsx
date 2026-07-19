"use client";

import { MIN_SEARCH_QUERY_LENGTH } from "@/shared/config";
import { buildArticleDetailUrl, buildSearchUrl, cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { SearchPanel, useRecentSearches } from "@/widgets/search";
import { useRouter } from "next/navigation";
import { Suspense } from "react";
import { SearchResults, SearchResultsSkeleton } from "./search-results";

type SearchPageProps = {
  className?: string;
  query: string;
};

export function SearchPage({ className, query }: SearchPageProps) {
  const router = useRouter();
  const { terms, addTerm, removeTerm } = useRecentSearches();

  const trimmedQuery = query.trim();
  const hasQuery = trimmedQuery.length >= MIN_SEARCH_QUERY_LENGTH;

  return (
    <div className={cn("flex flex-col gap-2xl", className)}>
      <SearchPanel
        initialQuery={query}
        recentTerms={terms}
        showExploreSections={!hasQuery}
        onArticleSelect={handleArticleSelect}
        onItemSelect={handleSearch}
        onRemoveRecent={removeTerm}
        onSubmit={handleSearch}
      />
      {hasQuery && (
        <Suspense key={trimmedQuery} fallback={<SearchResultsSkeleton />}>
          <SearchResults
            query={trimmedQuery}
            empty={
              <Translation
                className="rounded-md border border-hairline bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
                as="p"
                tKey="search.empty-state"
                values={{ query: trimmedQuery }}
              />
            }
          />
        </Suspense>
      )}
    </div>
  );

  function handleSearch(term: string) {
    addTerm(term);
    router.push(buildSearchUrl(term));
  }

  function handleArticleSelect(articleId: string) {
    router.push(buildArticleDetailUrl(articleId));
  }
}
