"use client";

import { MIN_SEARCH_QUERY_LENGTH } from "@/shared/config";
import { useRouter } from "@/shared/i18n";
import { buildArticleDetailUrl, buildSearchUrl, cn } from "@/shared/lib";
import { Modal } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useRecentSearches } from "../model/use-recent-searches";
import { SearchKeyboardHints } from "./search-keyboard-hints";
import { SearchPanel } from "./search-panel";

type SearchModalProps = {
  className?: string;
  isOpen: boolean;
  onClose: () => void;
};

export function SearchModal({ className, isOpen, onClose }: SearchModalProps) {
  const t = useTranslations("search");
  const router = useRouter();
  const { terms, addTerm, removeTerm } = useRecentSearches();

  return (
    <Modal
      className={cn("gap-lg", className)}
      hideCloseButton
      isOpen={isOpen}
      position="top"
      size="lg"
      header={{
        className: "sr-only",
        title: t("aria-label.search-input"),
        description: t("input-placeholder", { min: MIN_SEARCH_QUERY_LENGTH }),
      }}
      onClose={onClose}
    >
      <SearchPanel
        recentTerms={terms}
        onArticleSelect={handleArticleSelect}
        onItemSelect={handleSearch}
        onRemoveRecent={removeTerm}
        onSubmit={handleSearch}
      />
      <SearchKeyboardHints />
    </Modal>
  );

  function handleSearch(term: string) {
    addTerm(term);
    onClose();
    router.push(buildSearchUrl(term));
  }

  function handleArticleSelect(articleId: string) {
    onClose();
    router.push(buildArticleDetailUrl(articleId));
  }
}
