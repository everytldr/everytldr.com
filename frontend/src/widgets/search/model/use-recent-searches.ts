"use client";

import { uniqBy } from "lodash-es";
import { useCallback } from "react";
import { useStorageState } from "synced-storage/react";

const STORAGE_KEY = "article-search:recent";
const MAX_TERMS = 5;

export function useRecentSearches() {
  const [terms, setTerms] = useStorageState<string[]>(STORAGE_KEY, [], {
    strategy: "localStorage",
  });

  const addTerm = useCallback(
    (term: string) => {
      const normalized = term.trim();
      if (normalized.length === 0) {
        return;
      }
      setTerms((prev) => {
        if (prev[0]?.toLowerCase() === normalized.toLowerCase()) {
          return prev;
        }
        return uniqBy([normalized, ...prev], (t) => t.toLowerCase()).slice(0, MAX_TERMS);
      });
    },
    [setTerms],
  );

  const removeTerm = useCallback(
    (term: string) => {
      setTerms((prev) => {
        const next = prev.filter((t) => t !== term);
        return next.length === prev.length ? prev : next;
      });
    },
    [setTerms],
  );

  return { terms, addTerm, removeTerm };
}
