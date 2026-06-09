"use client";

import { buildSearchUrl, cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { SearchPanel, useRecentSearches } from "@/widgets/search";
import { useRouter } from "next/navigation";

type SearchPageProps = {
  className?: string;
  query: string;
};

export function SearchPage({ className, query }: SearchPageProps) {
  const router = useRouter();
  const { terms, addTerm, removeTerm } = useRecentSearches();

  const hasQuery = query.trim().length > 0;

  return (
    // TODO: Implement this page
    <div className={cn("flex flex-col gap-2xl", className)}>
      <SearchPanel
        initialQuery={query}
        recentTerms={terms}
        showExploreSections={!hasQuery}
        onItemSelect={handleSearch}
        onRemoveRecent={removeTerm}
        onSubmit={handleSearch}
      />
      {hasQuery && (
        <Translation
          className="rounded-md border border-hairline bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
          as="p"
          tKey="search.result-placeholder-body"
        />
      )}
    </div>
  );

  function handleSearch(term: string) {
    addTerm(term);
    router.push(buildSearchUrl(term));
  }
}
