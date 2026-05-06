import { Link, locales } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Logo } from "@/shared/ui";
import { Settings } from "lucide-react";
import { getLocale } from "next-intl/server";
import { ThemeToggle } from "./theme-toggle";

type HeaderProps = {
  className?: string;
};

export async function Header({ className }: HeaderProps) {
  const currentLocale = await getLocale();
  const otherLocale = locales.find((locale) => locale !== currentLocale);

  return (
    <header
      className={cn("sticky top-0 z-40 h-14 border-b border-hairline bg-canvas md:h-16", className)}
    >
      <Container className="flex h-full items-center justify-between">
        <Link href="/" aria-label="everytldr">
          <Logo className="[&_svg]:h-7 [&_svg]:w-auto" />
        </Link>

        <div className="flex items-center gap-2">
          {otherLocale && (
            <Link
              href="/"
              locale={otherLocale}
              className="hidden h-8 items-center rounded-full bg-surface-soft px-3 text-button-sm text-ink transition-colors hover:bg-surface-strong md:inline-flex"
            >
              {currentLocale.toUpperCase()}
            </Link>
          )}
          <ThemeToggle />
          <button
            type="button"
            aria-label="Settings"
            className="inline-flex size-9 items-center justify-center rounded-full bg-surface-strong text-ink transition-colors hover:bg-surface-soft"
          >
            <Settings className="size-4" />
          </button>
        </div>
      </Container>
    </header>
  );
}
