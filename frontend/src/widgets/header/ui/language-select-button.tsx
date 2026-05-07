import type { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { IconButton } from "@/shared/ui";
import { ChevronDown, Globe } from "lucide-react";
import { getTranslations } from "next-intl/server";

type LanguageSelectButtonProps = {
  className?: string;
  locale: Locale;
};

export async function LanguageSelectButton({ className, locale }: LanguageSelectButtonProps) {
  const t = await getTranslations("header");

  return (
    <div className={cn(className)}>
      {/* TODO: 모바일 언어 BottomSheet 연결 */}
      <IconButton className="pc:hidden" Icon={Globe} aria-label={t("language")} />
      {/* TODO: 데스크탑 언어 Dropdown 연결 */}
      <button
        className="hidden h-9 cursor-pointer items-center gap-xs rounded-full border border-hairline bg-canvas px-md text-button-sm text-body transition-colors outline-none hover:bg-surface-soft hover:text-ink focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:bg-surface-strong active:text-ink pc:inline-flex dark:hover:bg-surface-strong dark:active:bg-surface-pressed"
        type="button"
        aria-label={t("language")}
      >
        <Globe className="size-4" />
        <span>{locale.toUpperCase()}</span>
        <ChevronDown className="size-4" />
      </button>
    </div>
  );
}
