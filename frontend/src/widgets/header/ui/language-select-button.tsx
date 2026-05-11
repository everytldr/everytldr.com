"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import { isLocale, Locale, usePathname, useRouter } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import {
  BottomSheet,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
  IconButton,
  RadioGroup,
  RadioGroupIndicator,
  RadioGroupItem,
} from "@/shared/ui";
import { Check, ChevronDown, Globe } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

type Language = {
  code: Locale;
  glyph: string;
  native: string;
};

const LANGUAGES: readonly Language[] = [
  { code: Locale.En, glyph: "EN", native: "English" },
  { code: Locale.Ko, glyph: "KO", native: "한국어" },
];

type LanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

export function LanguageSelectButton({ className, locale }: LanguageSelectButtonProps) {
  const isCoarsePointer = useIsCoarsePointer();
  const router = useRouter();
  const pathname = usePathname();

  function handleSelect(next: Locale) {
    if (next === locale) {
      return;
    }
    router.replace(pathname, { locale: next });
  }

  if (isCoarsePointer) {
    return (
      <MobileLanguageSelectButton className={className} locale={locale} onSelect={handleSelect} />
    );
  }

  return (
    <DesktopLanguageSelectButton className={className} locale={locale} onSelect={handleSelect} />
  );
}

type LanguageSelectInnerProps = LanguageSelectButtonProps & {
  onSelect: (next: Locale) => void;
};

type MobileLanguageSelectButtonProps = LanguageSelectInnerProps;

function MobileLanguageSelectButton({
  className,
  locale,
  onSelect,
}: MobileLanguageSelectButtonProps) {
  const t = useTranslations("header");
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className={cn(className)}>
      <IconButton Icon={Globe} aria-label={t("language")} onClick={() => setIsOpen(true)} />
      <BottomSheet
        isOpen={isOpen}
        header={{ title: t("language") }}
        onClose={() => setIsOpen(false)}
      >
        <RadioGroup
          className="flex flex-col gap-2xs px-md pb-md"
          value={locale}
          aria-label={t("language")}
          onValueChange={(value) => isLocale(value) && onSelect(value)}
        >
          {LANGUAGES.map((lang) => (
            <RadioGroupItem
              key={lang.code}
              className="flex h-12 items-center justify-between gap-sm rounded-md px-md hover:bg-surface-soft active:bg-surface-strong dark:hover:bg-surface-strong dark:active:bg-surface-pressed"
              value={lang.code}
              onClick={() => setIsOpen(false)}
            >
              <span className="flex items-center gap-sm">
                <span className="rounded-xs border border-hairline px-xs py-2xs font-mono text-caption-mono text-meta">
                  {lang.glyph}
                </span>
                <span className="text-button-md text-ink">{lang.native}</span>
              </span>
              <RadioGroupIndicator>
                <Check className="size-5 text-ink" />
              </RadioGroupIndicator>
            </RadioGroupItem>
          ))}
        </RadioGroup>
      </BottomSheet>
    </div>
  );
}

type DesktopLanguageSelectButtonProps = LanguageSelectInnerProps;

function DesktopLanguageSelectButton({
  className,
  locale,
  onSelect,
}: DesktopLanguageSelectButtonProps) {
  const t = useTranslations("header");

  return (
    <div className={cn(className)}>
      <DropdownMenu>
        <DropdownMenuTrigger
          className="inline-flex h-9 cursor-pointer items-center gap-xs rounded-full border border-hairline bg-canvas px-md text-button-sm text-body transition-colors outline-none hover:bg-surface-soft hover:text-ink focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong active:text-ink data-[state=open]:bg-surface-soft data-[state=open]:text-ink dark:hover:bg-surface-strong dark:active:bg-surface-pressed dark:data-[state=open]:bg-surface-strong [&[data-state=open]>svg:last-child]:rotate-180"
          aria-label={`${t("language")}: ${locale.toUpperCase()}`}
        >
          <Globe className="size-4" />
          <span>{locale.toUpperCase()}</span>
          <ChevronDown className="size-4 transition-transform" />
        </DropdownMenuTrigger>
        <DropdownMenuContent className="min-w-40" align="end">
          <DropdownMenuRadioGroup
            value={locale}
            onValueChange={(value) => isLocale(value) && onSelect(value)}
          >
            {LANGUAGES.map((lang) => (
              <DropdownMenuRadioItem key={lang.code} value={lang.code}>
                {lang.native}
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
