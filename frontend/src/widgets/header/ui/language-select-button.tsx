"use client";

import { Locale, usePathname, useRouter } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { IconButton, ResponsiveSelector, type SelectorOption } from "@/shared/ui";
import { ChevronDown, Globe } from "lucide-react";
import { useTranslations } from "next-intl";

type Language = {
  code: Locale;
  glyph: string;
  native: string;
};

const LANGUAGES: readonly Language[] = [
  { code: Locale.En, glyph: "EN", native: "English" },
  { code: Locale.Ko, glyph: "KO", native: "한국어" },
];

const LANGUAGE_OPTIONS: readonly SelectorOption<Locale>[] = LANGUAGES.map((lang) => ({
  value: lang.code,
  content: <LanguageOptionContent glyph={lang.glyph} native={lang.native} />,
}));

type LanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

export function LanguageSelectButton({ className, locale }: LanguageSelectButtonProps) {
  const t = useTranslations("header");
  const router = useRouter();
  const pathname = usePathname();

  function handleChange(next: Locale) {
    router.replace(pathname, { locale: next });
  }

  return (
    <ResponsiveSelector<Locale>
      className={className}
      value={locale}
      options={LANGUAGE_OPTIONS}
      title={t("language")}
      mobileTrigger={({ openSheet }) => (
        <IconButton Icon={Globe} aria-label={t("language")} onClick={openSheet} />
      )}
      desktopTrigger={() => (
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
      onValueChange={handleChange}
    />
  );
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
