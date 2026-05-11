"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import { Locale, usePathname, useRouter } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import {
  BottomSheet,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
  IconButton,
} from "@/shared/ui";
import { Check, ChevronDown, Globe } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

type Language = {
  code: Locale;
  native: string;
};

const LANGUAGES: readonly Language[] = [
  { code: Locale.En, native: "English" },
  { code: Locale.Ko, native: "한국어" },
];

type LanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

export function LanguageSelectButton({ className, locale }: LanguageSelectButtonProps) {
  const isCoarsePointer = useIsCoarsePointer();

  if (isCoarsePointer) {
    return <MobileLanguageSelectButton className={className} locale={locale} />;
  }

  return <DesktopLanguageSelectButton className={className} locale={locale} />;
}

type MobileLanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

function MobileLanguageSelectButton({ className, locale }: MobileLanguageSelectButtonProps) {
  const t = useTranslations("header");
  const router = useRouter();
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className={cn(className)}>
      <IconButton Icon={Globe} aria-label={t("language")} onClick={() => setIsOpen(true)} />
      <BottomSheet
        isOpen={isOpen}
        header={{ title: t("language") }}
        onClose={() => setIsOpen(false)}
      >
        <div className="flex flex-col gap-2xs px-md pb-md">
          {LANGUAGES.map((lang) => {
            const isCurrent = lang.code === locale;
            return (
              <button
                key={lang.code}
                className="flex h-12 cursor-pointer items-center justify-between gap-sm rounded-md px-md text-body-md text-ink transition-colors outline-none hover:bg-surface-soft focus-visible:ring-2 focus-visible:ring-primary active:bg-surface-strong dark:hover:bg-surface-strong dark:active:bg-surface-pressed"
                type="button"
                aria-current={isCurrent}
                onClick={() => handleSelect(lang.code)}
              >
                <span>{lang.native}</span>
                {isCurrent && <Check className="size-5 text-primary" />}
              </button>
            );
          })}
        </div>
      </BottomSheet>
    </div>
  );

  function handleSelect(next: Locale) {
    setIsOpen(false);
    if (next === locale) {
      return;
    }
    router.replace(pathname, { locale: next });
  }
}

type DesktopLanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

function DesktopLanguageSelectButton({ className, locale }: DesktopLanguageSelectButtonProps) {
  const t = useTranslations("header");
  const router = useRouter();
  const pathname = usePathname();

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
          <DropdownMenuRadioGroup value={locale} onValueChange={handleSelect}>
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

  function handleSelect(value: string) {
    const target = LANGUAGES.find((lang) => lang.code === value);
    if (!target || target.code === locale) {
      return;
    }
    router.replace(pathname, { locale: target.code });
  }
}
