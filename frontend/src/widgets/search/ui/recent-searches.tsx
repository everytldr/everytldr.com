"use client";

import { cn } from "@/shared/lib";
import { Chip, Translation } from "@/shared/ui";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";

type RecentSearchesProps = {
  className?: string;
  terms: ReadonlyArray<string>;
  onItemSelect: (term: string) => void;
  onRemove: (term: string) => void;
};

export function RecentSearches({ className, terms, onItemSelect, onRemove }: RecentSearchesProps) {
  return (
    <section className={cn("flex flex-col gap-sm", className)}>
      <Translation className="text-title-sm text-ink" as="h3" tKey="search.section.recent" />
      <ul className="flex flex-wrap gap-xs">
        {terms.map((term) => (
          <li key={term}>
            <RecentChip
              term={term}
              onRemove={() => onRemove(term)}
              onSelect={() => onItemSelect(term)}
            />
          </li>
        ))}
      </ul>
    </section>
  );
}

type RecentChipProps = {
  className?: string;
  term: string;
  onSelect: () => void;
  onRemove: () => void;
};

function RecentChip({ className, term, onSelect, onRemove }: RecentChipProps) {
  const t = useTranslations("search");

  return (
    <Chip className={className} asChild isSelected={false}>
      <span>
        <button
          className="outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas"
          type="button"
          data-search-nav
          onClick={onSelect}
        >
          {term}
        </button>
        <button
          className="inline-flex size-md items-center justify-center rounded-full text-meta transition-colors outline-none hover:text-ink focus-visible:ring-2 focus-visible:ring-primary"
          type="button"
          aria-label={`${t("aria-label.remove-recent")}: ${term}`}
          onClick={onRemove}
        >
          <X className="size-sm" />
        </button>
      </span>
    </Chip>
  );
}
