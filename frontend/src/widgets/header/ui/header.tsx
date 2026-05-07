import { Link, Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, IconButton, Input, Logo } from "@/shared/ui";
import { Settings } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { ThemeToggle } from "./theme-toggle";

type HeaderProps = {
  className?: string;
  locale: Locale;
};

export async function Header({ className, locale }: HeaderProps) {
  const t = await getTranslations("header");

  return (
    <header className={cn("z-40 bg-canvas", className)}>
      <Container className="flex h-14 items-center justify-between gap-md md:h-16">
        <Link href="/" aria-label="everytldr">
          <Logo />
        </Link>

        <div className="flex items-center gap-x-xs">
          <Input
            className="w-60"
            variant="search"
            type="search"
            placeholder={<SearchInputPlaceholer locale={locale} />}
          />
          <ThemeToggle className="hidden md:inline-flex" />
          <IconButton Icon={Settings} aria-label={t("settings")} />
        </div>
      </Container>
    </header>
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
