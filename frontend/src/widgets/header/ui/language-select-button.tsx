"use client";

import { Locale, usePathname, useRouter } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Chip, IconButton, ResponsiveSelector } from "@/shared/ui";
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
      title={t("language")}
      value={locale}
      multiple={false}
      options={[
        { value: Locale.En, content: "English" },
        { value: Locale.Ko, content: "한국어" },
      ]}
      renderMobileTrigger={({ open }) => (
        <IconButton
          Icon={Globe}
          aria-label={`${t("language")}: ${locale.toUpperCase()}`}
          onClick={open}
        />
      )}
      renderDesktopTrigger={({ isOpen }) => (
        <Chip asChild isSelected={false}>
          <button type="button" aria-label={`${t("language")}: ${locale.toUpperCase()}`}>
            <Globe className="size-md" />
            <span>{locale.toUpperCase()}</span>
            <ChevronDown
              className={cn("size-md transition-transform", isOpen && "rotate-180")}
              aria-hidden
            />
          </button>
        </Chip>
      )}
      onChange={handleChange}
    />
  );

  function handleChange(next: Locale) {
    router.replace(pathname, { locale: next });
  }
}
