import { Link, type Locale } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Logo } from "@/shared/ui";
import { SearchTrigger } from "@/widgets/search";
import { LanguageSelectButton } from "./language-select-button";
import { ThemeToggle } from "./theme-toggle";

type HeaderProps = {
  className?: string;
  locale: Locale;
};

export function Header({ className, locale }: HeaderProps) {
  return (
    <header className={cn("z-40 bg-canvas", className)}>
      <Container className="flex h-14 items-center justify-between gap-md pc:h-16">
        <Link href="/" prefetch={false} aria-label="everytldr">
          <Logo />
        </Link>

        <div className="flex items-center gap-x-xs">
          <SearchTrigger />
          <ThemeToggle />
          <LanguageSelectButton locale={locale} />
        </div>
      </Container>
    </header>
  );
}
