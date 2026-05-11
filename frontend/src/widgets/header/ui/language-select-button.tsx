"use client";

import { Locale, usePathname, useRouter } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { IconButton, ResponsiveSelector } from "@/shared/ui";
import { ChevronDown, Globe } from "lucide-react";
import { useTranslations } from "next-intl";

type LanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

export function LanguageSelectButton({ className, locale }: LanguageSelectButtonProps) {
  const t = useTranslations("header");
  const router = useRouter();
  const pathname = usePathname();

  return (
    <ResponsiveSelector
      className={className}
      value={locale}
      title={t("language")}
      options={[
        { value: Locale.En, content: <LanguageOptionContent glyph="EN" native="English" /> },
        { value: Locale.Ko, content: <LanguageOptionContent glyph="KO" native="한국어" /> },
      ]}
      renderMobileTrigger={({ open }) => (
        <IconButton Icon={Globe} aria-label={t("language")} onClick={open} />
      )}
      renderDesktopTrigger={() => (
        <button
          className="inline-flex h-9 cursor-pointer items-center gap-xs rounded-full border border-hairline bg-canvas px-md text-button-sm text-body transition-colors outline-none hover:bg-surface-soft hover:text-ink focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong active:text-ink data-[state=open]:bg-surface-soft data-[state=open]:text-ink dark:hover:bg-surface-strong dark:active:bg-surface-pressed dark:data-[state=open]:bg-surface-strong [&[data-state=open]>svg:last-child]:rotate-180"
          type="button"
          aria-label={`${t("language")}: ${locale.toUpperCase()}`}
        >
          <Globe className="size-4" />
          <span>{locale.toUpperCase()}</span>
          <ChevronDown className="size-4 transition-transform" />
        </button>
      )}
      onChange={handleChange}
    />
  );

  function handleChange(next: Locale) {
    router.replace(pathname, { locale: next });
  }
}

type LanguageOptionContentProps = {
  className?: string;
  glyph: string;
  native: string;
};

function LanguageOptionContent({ className, glyph, native }: LanguageOptionContentProps) {
  return (
    <span className={cn("flex items-center gap-sm", className)}>
      <span className="rounded-xs border border-hairline px-xs py-2xs font-mono text-caption-mono text-meta">
        {glyph}
      </span>
      <span>{native}</span>
    </span>
  );
}
