import { Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { IconButton, Input } from "@/shared/ui";
import { Search } from "lucide-react";
import { getTranslations } from "next-intl/server";

type ArticleSearchButtonProps = {
  className?: string;
  locale: Locale;
};

export async function ArticleSearchButton({ className, locale }: ArticleSearchButtonProps) {
  const t = await getTranslations("header");

  return (
    <div className={cn(className)}>
      {/* TODO: 모바일 검색 모달 연결 */}
      <IconButton className="pc:hidden" Icon={Search} aria-label={t("search-placeholder")} />
      {/* TODO: 데스크탑 검색 모달 연결 */}
      <Input
        className="hidden w-60 pc:block"
        variant="search"
        type="search"
        placeholder={<SearchInputPlaceholer locale={locale} />}
      />
    </div>
  );
}

type SearchInputPlaceholer = {
  className?: string;
  locale: Locale;
};

function SearchInputPlaceholer({ className, locale }: SearchInputPlaceholer) {
  return (
    <div className={cn(className)}>
      {locale === Locale.En ? (
        <>
          <kbd>/</kbd> to search
        </>
      ) : (
        <>
          <kbd>/</kbd>를 눌러 검색하세요.
        </>
      )}
    </div>
  );
}
