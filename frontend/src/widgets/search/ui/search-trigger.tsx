"use client";

import { Link } from "@/shared/i18n";
import { cn, isEditableElement } from "@/shared/lib";
import { Input } from "@/shared/ui";
import { Search } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useState, type KeyboardEvent } from "react";
import { SearchModal } from "./search-modal";

type SearchTriggerProps = {
  className?: string;
};

export function SearchTrigger({ className }: SearchTriggerProps) {
  const t = useTranslations("search");
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (isOpen) {
      return;
    }
    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key !== "/" || event.metaKey || event.ctrlKey || event.altKey) {
        return;
      }
      if (isEditableElement(event.target)) {
        return;
      }
      event.preventDefault();
      setIsOpen(true);
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen]);

  return (
    <div className={cn(className)}>
      <Link
        className="inline-flex size-9 cursor-pointer items-center justify-center rounded-full bg-surface-soft text-ink transition-colors outline-none hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary active:bg-surface-pressed pc:hidden"
        href="/search"
        aria-label={t("aria-label.search-input")}
      >
        <Search className="pointer-events-none size-4.5" />
      </Link>
      <Input
        className="hidden w-60 pc:block"
        variant="search"
        placeholder={<TriggerPlaceholder />}
        readOnly
        type="search"
        aria-label={t("aria-label.search-input")}
        onMouseDown={(event) => {
          event.preventDefault();
          setIsOpen(true);
        }}
        onKeyDown={handleInputKeyDown}
      />
      <SearchModal isOpen={isOpen} onClose={() => setIsOpen(false)} />
    </div>
  );

  function handleInputKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter" || event.key === " " || event.key === "/") {
      event.preventDefault();
      setIsOpen(true);
    }
  }
}

function TriggerPlaceholder() {
  const t = useTranslations("search");
  return (
    <span className="inline-flex items-center gap-2xs truncate">
      {t.rich("trigger-placeholder", {
        kbd: (chunks) => (
          <kbd className="inline-flex h-md min-w-md items-center justify-center rounded-xs bg-surface-strong text-micro text-meta">
            {chunks}
          </kbd>
        ),
      })}
    </span>
  );
}
